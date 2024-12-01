package foundry.veil.api.resource.editor;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import foundry.veil.Veil;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.api.client.render.framebuffer.FramebufferDefinition;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.molang.VeilMolang;
import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.type.FramebufferResource;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import gg.moonflower.molangcompiler.api.exception.MolangRuntimeException;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import imgui.*;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Locale;
import java.util.function.Consumer;

public class FramebufferFileEditor implements ResourceFileEditor<FramebufferResource> {

    private static final StringBuilder BUILDER = new StringBuilder();
    private static final MolangRuntime RUNTIME = MolangRuntime.runtime()
            .setQuery("screen_width", MolangExpression.of(() -> (float) Minecraft.getInstance().getWindow().getWidth()))
            .setQuery("screen_height", MolangExpression.of(() -> (float) Minecraft.getInstance().getWindow().getHeight()))
            .create();

    private final ImBoolean open;
    private FramebufferResource resource;
    private FramebufferDefinitionBuilder builder;

    private final ImString widthInput = new ImString();
    private final ImString heightInput = new ImString();
    private BitSet enabledBuffers;

    private int attachmentIndex;
    private FramebufferAttachmentDefinition.Type type;
    private FramebufferAttachmentDefinition.Format format;
    private final ImString formatInput = new ImString();
    private FramebufferAttachmentDefinition.DataType dataType;
    private final ImString dataTypeInput = new ImString();
    private final ImBoolean linear = new ImBoolean();
    private int levels;
    private final ImInt levelsInput = new ImInt();
    private final ImString name = new ImString();

    public FramebufferFileEditor() {
        this.open = new ImBoolean(false);
    }

    @Override
    public void render() {
        if (this.resource == null || !this.open.get()) {
            return;
        }

        if (ImGui.begin("Framebuffer Editor: " + this.resource.resourceInfo().fileName() + "###framebuffer_editor")) {
            float definitionWidth = 1;
            float definitionHeight = 1;
            try {
                definitionWidth = RUNTIME.resolve(this.builder.getWidth());
                definitionHeight = RUNTIME.resolve(this.builder.getHeight());
            } catch (MolangRuntimeException ignored) {
            }

            float lineHeight = ImGui.getTextLineHeightWithSpacing();
            float boxWidth;
            float boxHeight;
            if (definitionWidth >= definitionHeight) {
                boxWidth = ImGui.getContentRegionAvailX() / 2;
                boxHeight = boxWidth * definitionHeight / definitionWidth;

                if (boxHeight > ImGui.getContentRegionAvailY() - 225) {
                    boxHeight = ImGui.getContentRegionAvailY() - 225;
                    boxWidth = Math.max(1, boxHeight * definitionWidth / definitionHeight);
                }
            } else {
                boxHeight = ImGui.getContentRegionAvailY() / 2;
                boxWidth = Math.max(1, boxHeight * definitionWidth / definitionHeight);
            }

            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
            if (ImGui.beginChild("##size", boxWidth, boxHeight, true)) {
                FramebufferManager framebufferManager = VeilRenderSystem.renderer().getFramebufferManager();
                ResourceLocation id = FramebufferManager.FRAMEBUFFER_LISTER.fileToId(this.resource.resourceInfo().location());
                AdvancedFbo fbo = framebufferManager.getFramebuffer(id);

                if (fbo != null) {
                    int textureId = 0;
                    if (this.attachmentIndex == 0) {
                        if (fbo.isDepthTextureAttachment()) {
                            textureId = fbo.getDepthTextureAttachment().getId();
                        }
                    } else {
                        if (fbo.isColorTextureAttachment(this.attachmentIndex - 1)) {
                            textureId = fbo.getColorTextureAttachment(this.attachmentIndex - 1).getId();
                        }
                    }
                    if (textureId > 0) {
                        ImGui.image(textureId, boxWidth, boxHeight, 0, 1, 1, 0);
                    }
                }
            }
            ImGui.endChild();
            ImGui.popStyleVar();

            VeilImGuiUtil.textCentered(Integer.toString((int) definitionWidth), boxWidth);

            if (ImGui.beginChild("##panel", ImGui.getContentRegionAvailX() / 2, ImGui.getContentRegionAvailY())) {
                ImGui.text("Framebuffer Settings:");
                this.renderFramebufferSettings();
                ImGui.newLine();

                ImGui.text("Attachment Settings:");
                this.renderAttachmentSettings();
            }
            ImGui.endChild();

            String heightString = Integer.toString((int) definitionHeight);
            ImFont font = ImGui.getFont();
            float width = font.calcTextSizeAX(ImGui.getFontSize(), Float.MAX_VALUE, 0, heightString);
            ImGui.setCursorPos(ImGui.getCursorStartPosX() + boxWidth, ImGui.getCursorStartPosY() + boxHeight / 2 + width / 2);

            drawVerticalText(heightString);

            ImGui.setCursorPos(ImGui.getCursorStartPosX() + ImGui.getContentRegionMaxX() / 2 + lineHeight, ImGui.getCursorStartPosY());
            ImGui.beginGroup();

            if (ImGui.beginListBox("##buffers", ImGui.getContentRegionAvailX(), ImGui.getContentRegionAvailY() - ImGui.getFrameHeight() - ImGui.getStyle().getFramePaddingY() * 2)) {
                float itemWidth = ImGui.getContentRegionAvailX();

                boolean useDepth = this.enabledBuffers.get(0);
                if (useDepth) {
                    if (ImGui.invisibleButton("##depth", itemWidth, lineHeight * 4)) {
                        this.setAttachment(0);
                    }

                    if (this.builder.getDepthBuffer() == null) {
                        this.builder.setDepthBuffer(new FramebufferAttachmentDefinition(FramebufferAttachmentDefinition.Type.TEXTURE, FramebufferAttachmentDefinition.Format.DEPTH_COMPONENT, FramebufferAttachmentDefinition.DataType.FLOAT, true, false, 0, null));
                    }
                    drawBuffer("Depth Buffer", this.builder.getDepthBuffer(), itemWidth, this.attachmentIndex == 0);
                }

                for (int i = 0; i < this.enabledBuffers.size() - 1; i++) {
                    boolean enabled = this.enabledBuffers.get(i + 1);
                    if (!enabled) {
                        continue;
                    }

                    if (ImGui.invisibleButton("##color" + i, itemWidth, lineHeight * 4)) {
                        this.setAttachment(i + 1);
                    }

                    FramebufferAttachmentDefinition[] colorBuffers = this.builder.getColorBuffers();
                    if (colorBuffers[i] == null) {
                        this.builder.setColorBuffer(i, new FramebufferAttachmentDefinition(FramebufferAttachmentDefinition.Type.TEXTURE, FramebufferAttachmentDefinition.Format.RGBA8, FramebufferAttachmentDefinition.DataType.UNSIGNED_BYTE, false, false, 0, null));
                    }
                    drawBuffer("Color Buffer " + i, colorBuffers[i], itemWidth, this.attachmentIndex == i + 1);
                }
                ImGui.endListBox();
            }

            if (ImGui.checkbox("Use Depth", this.enabledBuffers.get(0))) {
                this.enabledBuffers.flip(0);
                if (this.attachmentIndex == 0 && !this.enabledBuffers.get(0)) {
                    this.setAttachment(1);
                }
            }
            ImGui.sameLine();
            if (ImGui.checkbox("Auto Clear", this.builder.isAutoClear())) {
                this.builder.setAutoClear(!this.builder.isAutoClear());
            }

            ImGui.endGroup();
        }
        ImGui.end();
    }

    private static void drawBuffer(String name, FramebufferAttachmentDefinition attachment, float width, boolean selected) {
        ImGuiStyle style = ImGui.getStyle();
        float lineHeight = ImGui.getTextLineHeightWithSpacing();
        boolean hovered = ImGui.isItemHovered();
        boolean active = selected || ImGui.isItemActive();

        ImGui.sameLine();
        ImGui.setCursorPosX(ImGui.getCursorStartPosX());

        ImVec4 col = new ImVec4();
        style.getColor(active ? ImGuiCol.ButtonActive : hovered ? ImGuiCol.ButtonHovered : ImGuiCol.Button, col);
        int colorU32 = ImGui.getColorU32(col.x, col.y, col.z, col.w);
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        ImGui.getWindowDrawList().addRectFilled(x, y, x + width, y + lineHeight * 4, colorU32);

        ImGui.setCursorPosX(ImGui.getCursorPosX() + style.getFramePaddingX());
        ImGui.beginGroup();

        boolean texture = attachment.type() == FramebufferAttachmentDefinition.Type.TEXTURE;
        text(builder -> {
            builder.append(name);
            if (attachment.name() != null) {
                builder.append(" (").append(attachment.name()).append(")");
            }
        });
        text(builder -> {
            builder.append(attachment.type().getDisplayName());
            if (texture) {
                builder.append(attachment.linear() ? " Linear" : " Nearest");
            }
        });
        text(builder -> builder.append(attachment.format().name()).append(" ").append(attachment.dataType().name()));
        text(builder -> builder.append(attachment.levels()).append(texture ? " Mipmaps" : " Samples"));

        ImGui.endGroup();
    }

    private static void text(Consumer<StringBuilder> text) {
        text.accept(BUILDER);
        ImGui.text(BUILDER.toString());
        BUILDER.setLength(0);
    }

    private static void drawVerticalText(String next) {
        ImFont font = ImGui.getFont();
        float pad = ImGui.getStyle().getFramePaddingX();
        float posX = ImGui.getCursorScreenPosX() + pad;
        float posY = ImGui.getCursorScreenPosY() + pad;

        ImDrawList drawList = ImGui.getWindowDrawList();
        for (int i = 0; i < next.length(); i++) {
            int codePoint = next.codePointAt(i);
            ImFontGlyph glyph = font.findGlyph(codePoint);

            posY -= font.getCharAdvance(codePoint);
            drawList.primReserve(6, 4);
            drawList.primQuadUV(
                    (int) (posX + glyph.getY1()),
                    (int) (posY + glyph.getX0()),
                    (int) (posX + glyph.getY1()),
                    (int) (posY + glyph.getX1()),
                    (int) (posX + glyph.getY0()),
                    (int) (posY + glyph.getX1()),
                    (int) (posX + glyph.getY0()),
                    (int) (posY + glyph.getX0()),

                    glyph.getU1(),
                    glyph.getV1(),
                    glyph.getU0(),
                    glyph.getV1(),
                    glyph.getU0(),
                    glyph.getV0(),
                    glyph.getU1(),
                    glyph.getV0(),
                    -1);
        }
    }

    private void renderFramebufferSettings() {
        if (ImGui.inputText("Width", this.widthInput, ImGuiInputTextFlags.EnterReturnsTrue)) {
            try {
                this.builder.setWidth(VeilMolang.get().compile(this.widthInput.get()));
            } catch (MolangSyntaxException ignored) {
            }
            this.updateSize();
        }
        if (ImGui.inputText("Height", this.heightInput, ImGuiInputTextFlags.EnterReturnsTrue)) {
            try {
                this.builder.setHeight(VeilMolang.get().compile(this.heightInput.get()));
            } catch (MolangSyntaxException ignored) {
            }
            this.updateSize();
        }
    }

    private void renderAttachmentSettings() {
        if (ImGui.beginCombo("Type", this.type.getDisplayName())) {
            for (FramebufferAttachmentDefinition.Type type : FramebufferAttachmentDefinition.Type.values()) {
                if (ImGui.selectable(type.getDisplayName())) {
                    this.type = type;
                    this.saveAttachment();
                }
            }
            ImGui.endCombo();
        }
        if (ImGui.inputText("Format", this.formatInput, ImGuiInputTextFlags.EnterReturnsTrue)) {
            try {
                this.format = FramebufferAttachmentDefinition.Format.valueOf(this.formatInput.get().toUpperCase(Locale.ROOT));
                this.saveAttachment();
            } catch (IllegalArgumentException ignored) {
            }
            this.formatInput.set(this.format.name());
        }
        ImGui.beginDisabled(this.type == FramebufferAttachmentDefinition.Type.RENDER_BUFFER);
        if (ImGui.inputText("Data Type", this.dataTypeInput, ImGuiInputTextFlags.EnterReturnsTrue)) {
            try {
                this.dataType = FramebufferAttachmentDefinition.DataType.valueOf(this.dataTypeInput.get().toUpperCase(Locale.ROOT));
                this.saveAttachment();
            } catch (IllegalArgumentException ignored) {
            }
            this.dataTypeInput.set(this.dataType.name());
        }
        if (ImGui.checkbox("Linear", this.linear)) {
            this.saveAttachment();
        }
        ImGui.endDisabled();
        if (ImGui.sliderInt(this.type == FramebufferAttachmentDefinition.Type.RENDER_BUFFER ? "Samples" : "Mipmaps", this.levelsInput.getData(), 1, VeilRenderSystem.maxSamples())) {
            this.levels = this.levelsInput.get();
            this.saveAttachment();
        }
        if (ImGui.inputText("Name", this.name, ImGuiInputTextFlags.EnterReturnsTrue)) {
            this.saveAttachment();
        }
//        this.type = buffer.type();
//        this.format = buffer.format();
//        this.dataType = buffer.dataType();
//        this.linear = buffer.linear();
//        this.levels = buffer.levels();
//        this.name = buffer.name();
    }

    private void updateSize() {
        String width = this.builder.getWidth().toString();
        if (width.startsWith("return")) {
            this.widthInput.set(width.substring(7));
        } else {
            this.widthInput.set(width);
        }

        String height = this.builder.getHeight().toString();
        if (height.startsWith("return")) {
            this.heightInput.set(height.substring(7));
        } else {
            this.heightInput.set(height);
        }
        this.createFramebuffer();
    }

    private void saveAttachment() {
        if (this.attachmentIndex == 0) {
            this.builder.setDepthBuffer(new FramebufferAttachmentDefinition(this.type, this.format, this.dataType, true, this.linear.get(), this.levels, this.name.get()));
        } else {
            this.builder.setColorBuffer(this.attachmentIndex - 1, new FramebufferAttachmentDefinition(this.type, this.format, this.dataType, false, this.linear.get(), this.levels, this.name.get()));
        }
        this.createFramebuffer();
    }

    private void setAttachment(int attachmentIndex) {
        this.attachmentIndex = attachmentIndex;

        FramebufferAttachmentDefinition buffer;
        if (this.attachmentIndex == 0) {
            buffer = this.builder.getDepthBuffer();
        } else {
            FramebufferAttachmentDefinition[] colorBuffers = this.builder.getColorBuffers();
            if (attachmentIndex < 1 || attachmentIndex >= colorBuffers.length + 1) {
                return;
            }

            buffer = colorBuffers[attachmentIndex - 1];
        }

        if (buffer != null) {
            this.type = buffer.type();
            this.format = buffer.format();
            this.formatInput.set(this.format.name());
            this.dataType = buffer.dataType();
            this.dataTypeInput.set(this.dataType.name());
            this.linear.set(buffer.linear());
            this.levels = buffer.levels();
            this.name.set(buffer.name());
        }
    }

    private void createFramebuffer() {
        ResourceLocation id = FramebufferManager.FRAMEBUFFER_LISTER.fileToId(this.resource.resourceInfo().location());
        VeilRenderSystem.renderer().getFramebufferManager().setDefinition(id, this.builder.create());
    }

    @Override
    public void open(VeilEditorEnvironment environment, FramebufferResource resource) {
        this.open.set(true);
        this.resource = resource;

        try (BufferedReader reader = resource.resourceInfo().openAsReader(environment.getResourceManager())) {
            DataResult<FramebufferDefinition> result = FramebufferDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader));
            if (result.error().isPresent()) {
                throw new JsonParseException(result.error().get().message());
            }
            this.builder = new FramebufferDefinitionBuilder(result.result().orElseThrow());
        } catch (IOException e) {
            Veil.LOGGER.error("Failed to open resource: {}", resource.resourceInfo().location(), e);
            this.builder = new FramebufferDefinitionBuilder();
        }

        if (this.enabledBuffers == null) {
            this.enabledBuffers = new BitSet(VeilRenderSystem.maxColorAttachments() + 1);
        }

        FramebufferAttachmentDefinition[] colorBuffers = this.builder.getColorBuffers();
        this.enabledBuffers.set(0, this.builder.getDepthBuffer() != null);
        for (int i = 0; i < colorBuffers.length; i++) {
            this.enabledBuffers.set(i + 1, colorBuffers[i] != null);
        }

        this.updateSize();
        this.setAttachment(1);
    }

    private static class FramebufferDefinitionBuilder {

        private MolangExpression width;
        private MolangExpression height;
        private final FramebufferAttachmentDefinition[] colorBuffers;
        @Nullable
        private FramebufferAttachmentDefinition depthBuffer;
        private boolean autoClear;

        public FramebufferDefinitionBuilder() {
            this.width = FramebufferDefinition.DEFAULT_WIDTH;
            this.height = FramebufferDefinition.DEFAULT_HEIGHT;
            this.colorBuffers = new FramebufferAttachmentDefinition[VeilRenderSystem.maxColorAttachments()];
            this.depthBuffer = null;
            this.autoClear = true;
        }

        public FramebufferDefinitionBuilder(FramebufferDefinition definition) {
            this.width = definition.width();
            this.height = definition.height();
            this.colorBuffers = Arrays.copyOf(definition.colorBuffers(), VeilRenderSystem.maxColorAttachments());
            this.depthBuffer = definition.depthBuffer();
            this.autoClear = definition.autoClear();
        }

        public void setWidth(MolangExpression width) {
            this.width = width;
        }

        public void setHeight(MolangExpression height) {
            this.height = height;
        }

        public void setColorBuffer(int index, FramebufferAttachmentDefinition colorBuffer) {
            this.colorBuffers[index] = colorBuffer;
        }

        public void setDepthBuffer(@Nullable FramebufferAttachmentDefinition depthBuffer) {
            this.depthBuffer = depthBuffer;
        }

        public void setAutoClear(boolean autoClear) {
            this.autoClear = autoClear;
        }

        public MolangExpression getWidth() {
            return this.width;
        }

        public MolangExpression getHeight() {
            return this.height;
        }

        public FramebufferAttachmentDefinition[] getColorBuffers() {
            return this.colorBuffers;
        }

        public @Nullable FramebufferAttachmentDefinition getDepthBuffer() {
            return this.depthBuffer;
        }

        public boolean isAutoClear() {
            return this.autoClear;
        }

        public FramebufferDefinition create() {
            return new FramebufferDefinition(this.width, this.height, this.colorBuffers, this.depthBuffer, this.autoClear);
        }
    }
}
