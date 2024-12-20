package foundry.veil.api.resource.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
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
import foundry.veil.api.resource.VeilResourceManager;
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
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class FramebufferFileEditor implements ResourceFileEditor<FramebufferResource> {

    private static final StringBuilder BUILDER = new StringBuilder();
    private static final MolangRuntime RUNTIME = MolangRuntime.runtime()
            .setQuery("screen_width", MolangExpression.of(() -> (float) Minecraft.getInstance().getWindow().getWidth()))
            .setQuery("screen_height", MolangExpression.of(() -> (float) Minecraft.getInstance().getWindow().getHeight()))
            .create();

    private final ImBoolean open;
    private final VeilResourceManager resourceManager;
    private final FramebufferResource resource;
    private FramebufferDefinitionBuilder builder;

    private final ImString widthInput = new ImString();
    private final ImString heightInput = new ImString();
    private boolean useDepth;
    private int colorBuffers;

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

    public FramebufferFileEditor(VeilEditorEnvironment environment, FramebufferResource resource) {
        this.open = new ImBoolean(true);
        this.resourceManager = environment.getResourceManager();
        this.resource = resource;
        this.loadFromDisk();
    }

    @Override
    public void render() {
        if (ImGui.begin("Framebuffer Editor: " + this.resource.resourceInfo().fileName(), this.open)) {
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

                if (this.useDepth) {
                    if (ImGui.invisibleButton("##depth", itemWidth, lineHeight * 4)) {
                        this.setAttachment(0);
                    }

                    drawBuffer("Depth Buffer", this.builder.getOrCreateDepthBuffer(), itemWidth, this.attachmentIndex == 0);
                }

                for (int i = 0; i < this.colorBuffers; i++) {
                    if (ImGui.invisibleButton("##color" + i, itemWidth, lineHeight * 4)) {
                        this.setAttachment(i + 1);
                    }

                    drawBuffer("Color Buffer " + i, this.builder.getOrCreateColorBuffer(i), itemWidth, this.attachmentIndex == i + 1);
                }
                ImGui.endListBox();
            }

            if (ImGui.checkbox("Use Depth", this.useDepth)) {
                this.useDepth = !this.useDepth;
                if (this.attachmentIndex == 0 && !this.useDepth) {
                    this.setAttachment(1);
                }
                this.save();
            }

            ImGui.sameLine();
            if (ImGui.checkbox("Auto Clear", this.builder.isAutoClear())) {
                this.builder.setAutoClear(!this.builder.isAutoClear());
                this.save();
            }

            ImGui.sameLine();
            ImGui.beginDisabled(this.colorBuffers >= this.builder.colorBuffers.length);
            if (ImGui.button("+") && this.colorBuffers < this.builder.colorBuffers.length) {
                this.colorBuffers++;
                this.setAttachment(this.colorBuffers);
                this.save();
            }
            ImGui.endDisabled();

            ImGui.sameLine();
            ImGui.beginDisabled(this.colorBuffers <= 1);
            if (ImGui.button("-") && this.colorBuffers > 1) {
                this.colorBuffers--;
                this.builder.setColorBuffer(this.attachmentIndex - 1, null);
                this.setAttachment(Math.min(this.attachmentIndex, this.colorBuffers));
                this.save();
            }
            ImGui.endDisabled();

            ImGui.endGroup();
        }
        ImGui.end();
    }

    @Override
    public void loadFromDisk() {
        try (BufferedReader reader = this.resource.resourceInfo().openAsReader(this.resourceManager)) {
            DataResult<FramebufferDefinition> result = FramebufferDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader));
            if (result.error().isPresent()) {
                throw new JsonParseException(result.error().get().message());
            }
            this.builder = new FramebufferDefinitionBuilder(result.result().orElseThrow());
        } catch (IOException e) {
            Veil.LOGGER.error("Failed to open resource: {}", this.resource.resourceInfo().location(), e);
            this.builder = new FramebufferDefinitionBuilder();
        }

        FramebufferAttachmentDefinition[] colorBuffers = this.builder.getColorBuffers();
        this.useDepth = this.builder.getDepthBuffer() != null;
        this.colorBuffers = 0;
        for (FramebufferAttachmentDefinition colorBuffer : colorBuffers) {
            if (colorBuffer != null) {
                this.colorBuffers++;
            }
        }

        this.updateSize();
        this.setAttachment(Math.min(this.attachmentIndex, this.colorBuffers));
    }

    @Override
    public boolean isClosed() {
        return !this.open.get();
    }

    @Override
    public FramebufferResource getResource() {
        return this.resource;
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
        int length = next.length();
        drawList.primReserve(6 * length, 4 * length);
        for (int i = 0; i < length; i++) {
            int codePoint = next.codePointAt(i);
            ImFontGlyph glyph = font.findGlyph(codePoint);

            posY -= font.getCharAdvance(codePoint);
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
            this.save();
        }
        if (ImGui.inputText("Height", this.heightInput, ImGuiInputTextFlags.EnterReturnsTrue)) {
            try {
                this.builder.setHeight(VeilMolang.get().compile(this.heightInput.get()));
            } catch (MolangSyntaxException ignored) {
            }
            this.updateSize();
            this.save();
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
        if (ImGui.beginCombo("Data Type", this.dataType.name())) {
            for (FramebufferAttachmentDefinition.DataType type : FramebufferAttachmentDefinition.DataType.values()) {
                if (ImGui.selectable(type.name())) {
                    this.dataType = type;
                    this.saveAttachment();
                }
            }
            ImGui.endCombo();
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
    }

    private void saveAttachment() {
        String name = this.name.get().isBlank() ? null : this.name.get();
        if (this.attachmentIndex == 0) {
            this.builder.setDepthBuffer(new FramebufferAttachmentDefinition(this.type, this.format, this.dataType, true, this.linear.get(), this.levels, name));
        } else {
            this.builder.setColorBuffer(this.attachmentIndex - 1, new FramebufferAttachmentDefinition(this.type, this.format, this.dataType, false, this.linear.get(), this.levels, name));
        }
        this.save();
    }

    private void setAttachment(int attachmentIndex) {
        this.attachmentIndex = attachmentIndex;

        FramebufferAttachmentDefinition buffer;
        if (this.attachmentIndex == 0) {
            buffer = this.builder.getOrCreateDepthBuffer();
        } else {
            FramebufferAttachmentDefinition[] colorBuffers = this.builder.getColorBuffers();
            if (attachmentIndex < 1 || attachmentIndex >= colorBuffers.length + 1) {
                return;
            }

            buffer = this.builder.getOrCreateColorBuffer(attachmentIndex - 1);
        }

        this.type = buffer.type();
        this.format = buffer.format();
        this.formatInput.set(this.format.name());
        this.dataType = buffer.dataType();
        this.dataTypeInput.set(this.dataType.name());
        this.linear.set(buffer.linear());
        this.levels = buffer.levels();
        this.levelsInput.set(this.levels);
        if (buffer.name() != null) {
            this.name.set(buffer.name());
        } else {
            this.name.clear();
        }
    }

    private void save() {
        try {
            FramebufferDefinition definition = this.builder.create(this.useDepth, this.colorBuffers);
            DataResult<JsonElement> result = FramebufferDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition);
            if (result.error().isPresent()) {
                throw new JsonSyntaxException(result.error().get().message());
            }

            this.save(result.getOrThrow(), this.resourceManager, this.resource);
        } catch (Exception e) {
            Veil.LOGGER.error("Failed to save resource: {}", this.resource.resourceInfo().location(), e);
        }
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

        public void setColorBuffer(int index, @Nullable FramebufferAttachmentDefinition colorBuffer) {
            this.colorBuffers[index] = colorBuffer;
            if (colorBuffer == null && index < this.colorBuffers.length - 1) {
                System.arraycopy(this.colorBuffers, index + 1, this.colorBuffers, index, this.colorBuffers.length - index - 1);
            }
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

        public FramebufferAttachmentDefinition getOrCreateColorBuffer(int i) {
            FramebufferAttachmentDefinition buffer = this.colorBuffers[i];
            if (buffer != null) {
                return buffer;
            }

            return this.colorBuffers[i] = new FramebufferAttachmentDefinition(FramebufferAttachmentDefinition.Type.TEXTURE, FramebufferAttachmentDefinition.Format.RGBA8, FramebufferAttachmentDefinition.DataType.UNSIGNED_BYTE, false, false, 1, null);
        }

        public FramebufferAttachmentDefinition getOrCreateDepthBuffer() {
            return Objects.requireNonNullElseGet(this.depthBuffer, () -> this.depthBuffer = new FramebufferAttachmentDefinition(FramebufferAttachmentDefinition.Type.TEXTURE, FramebufferAttachmentDefinition.Format.DEPTH_COMPONENT, FramebufferAttachmentDefinition.DataType.FLOAT, true, false, 1, null));
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

        public FramebufferDefinition create(boolean useDepth, int buffers) {
            FramebufferAttachmentDefinition[] colorBuffers = IntStream.range(0, buffers)
                    .mapToObj(this::getOrCreateColorBuffer)
                    .toArray(FramebufferAttachmentDefinition[]::new);
            return new FramebufferDefinition(this.width, this.height, colorBuffers, useDepth ? this.getOrCreateDepthBuffer() : null, this.autoClear);
        }
    }
}
