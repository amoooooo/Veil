package foundry.veil.impl.client.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.editor.SingleWindowInspector;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.util.TextureDownloader;
import imgui.ImGui;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.NativeResource;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL20C.glIsTexture;
import static org.lwjgl.opengl.GL45C.GL_TEXTURE_TARGET;
import static org.lwjgl.opengl.GL45C.glGetTextureParameteri;

@ApiStatus.Internal
public class TextureInspector extends SingleWindowInspector {

    public static final Component TITLE = Component.translatable("inspector.veil.texture.title");
    public static final Component DOWNLOAD = Component.translatable("inspector.veil.texture.button.download");
    public static final Component POP_OUT = Component.translatable("inspector.veil.texture.toggle.pop_out");
    public static final Component FLIP_X = Component.translatable("inspector.veil.texture.toggle.flip_x");
    public static final Component FLIP_Y = Component.translatable("inspector.veil.texture.toggle.flip_y");
    public static final Component NO_TEXTURE = Component.translatable("inspector.veil.texture.asset.missing");
    private static final ResourceLocation DEBUG_CUBEMAP_SHADER = Veil.veilPath("debug/cubemap");

    private final IntSet texturesSet;
    private final Int2ObjectMap<OpenTexture> openTextures;
    private final Int2ObjectMap<CubemapStorage> cubemapStorage;
    private final ImBoolean flipX;
    private final ImBoolean flipY;
    private int[] textures;
    private int selectedTexture;
    private boolean downloadTextures;
    private CompletableFuture<?> downloadFuture;

    public TextureInspector() {
        this.texturesSet = new IntArraySet();
        this.openTextures = new Int2ObjectArrayMap<>();
        this.cubemapStorage = new Int2ObjectArrayMap<>();
        this.flipX = new ImBoolean();
        this.flipY = new ImBoolean();
        this.textures = new int[0];
        this.selectedTexture = 0;
        this.downloadFuture = null;
    }

    private void scanTextures() {
        this.texturesSet.clear();
        for (int i = 0; i < 10000; i++) {
            if (!glIsTexture(i)) {
                continue;
            }

            this.texturesSet.add(i);
        }

        if (this.textures.length != this.texturesSet.size()) {
            if (!this.texturesSet.contains(this.selectedTexture)) {
                this.selectedTexture = 0;
            }
            this.textures = this.texturesSet.toIntArray();
            this.openTextures.keySet().removeIf(a -> !this.texturesSet.contains(a));

            ObjectIterator<Int2ObjectMap.Entry<CubemapStorage>> iterator = this.cubemapStorage.int2ObjectEntrySet().iterator();
            while (iterator.hasNext()) {
                Int2ObjectMap.Entry<CubemapStorage> entry = iterator.next();
                if (!this.texturesSet.contains(entry.getIntKey())) {
                    entry.getValue().free();
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public Component getGroup() {
        return RENDERER_GROUP;
    }

    @Override
    protected void renderComponents() {
        this.scanTextures();

        int selectedId = this.selectedTexture < 0 || this.selectedTexture >= this.textures.length ? 0 : this.textures[this.selectedTexture];
        int[] value = {this.selectedTexture};

        ImGui.beginDisabled(this.textures.length == 0);
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() / 2);
        if (ImGui.sliderInt("##textures", value, 0, this.textures.length - 1, selectedId == 0 ? NO_TEXTURE.getString() : Integer.toString(selectedId))) {
            this.selectedTexture = value[0];
        }
        ImGui.endDisabled();
        ImGui.sameLine();

        ImGui.pushButtonRepeat(true);
        ImGui.beginDisabled(this.selectedTexture <= 0);
        if (ImGui.arrowButton("##left", ImGuiDir.Left)) {
            this.selectedTexture--;
        }
        ImGui.endDisabled();
        ImGui.beginDisabled(this.selectedTexture >= this.textures.length - 1);
        ImGui.sameLine(0.0f, ImGui.getStyle().getItemInnerSpacingX());
        if (ImGui.arrowButton("##right", ImGuiDir.Right)) {
            this.selectedTexture++;
        }
        ImGui.endDisabled();
        ImGui.popButtonRepeat();

        ImGui.beginDisabled(this.downloadFuture != null && !this.downloadFuture.isDone());
        ImGui.sameLine();
        if (ImGui.button(DOWNLOAD.getString())) {
            this.downloadTextures = true;
            this.downloadFuture = new CompletableFuture<>();
        }
        ImGui.endDisabled();

        ImGui.beginDisabled(this.openTextures.containsKey(selectedId) && this.openTextures.get(selectedId).visible.get());
        ImGui.sameLine(0.0f, ImGui.getStyle().getItemInnerSpacingX());
        if (ImGui.button(POP_OUT.getString())) {
            this.openTextures.put(selectedId, new OpenTexture(this.flipX.get(), this.flipY.get()));
        }
        ImGui.endDisabled();

        ImGui.sameLine(0.0f, ImGui.getStyle().getItemInnerSpacingX());
        ImGui.checkbox(FLIP_X.getString(), this.flipX);

        ImGui.sameLine(0.0f, ImGui.getStyle().getItemInnerSpacingX());
        ImGui.checkbox(FLIP_Y.getString(), this.flipY);

        if (selectedId != 0) {
            this.addImage(selectedId, this.flipX.get(), this.flipY.get());
        }
    }

    @Override
    public void render() {
        super.render();

        ObjectIterator<Int2ObjectMap.Entry<OpenTexture>> iterator = this.openTextures.int2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<OpenTexture> entry = iterator.next();
            int id = entry.getIntKey();

            OpenTexture texture = entry.getValue();
            if (!texture.visible.get()) {
                continue;
            }

            ImBoolean open = texture.open;
            if (!open.get()) {
                open.set(true);
                ImGui.setNextWindowSize(800, 600);
            }
            if (ImGui.begin(I18n.get("inspector.veil.texture.asset", id), open, ImGuiWindowFlags.NoSavedSettings)) {
                ImBoolean flipX = texture.flipX;
                ImBoolean flipY = texture.flipY;
                ImGui.checkbox(FLIP_X.getString(), flipX);
                ImGui.sameLine(0.0f, ImGui.getStyle().getItemInnerSpacingX());
                ImGui.checkbox(FLIP_Y.getString(), flipY);
                this.addImage(id, flipX.get(), flipY.get());
            }
            ImGui.end();

            if (!open.get()) {
                iterator.remove();
            }
        }
    }

    @Override
    public void renderLast() {
        super.renderLast();

        if (this.downloadTextures) {
            this.downloadTextures = false;

            try {
                Minecraft client = Minecraft.getInstance();
                Path outputFolder = Paths.get(client.gameDirectory.toURI()).resolve("debug-out");
                if (!Files.exists(outputFolder)) {
                    Files.createDirectories(outputFolder);
                } else {
                    Files.walkFileTree(outputFolder, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }

                List<CompletableFuture<?>> result = new LinkedList<>();
                for (int i : this.texturesSet) {
                    result.add(TextureDownloader.save(Integer.toString(i), outputFolder, i, false));
                }

                this.downloadFuture = CompletableFuture.allOf(result.toArray(new CompletableFuture[0])).thenRunAsync(() -> Util.getPlatform().openFile(outputFolder.toFile()), client);
            } catch (Exception e) {
                Veil.LOGGER.error("Failed to download textures", e);
            }
        }
    }

    @Override
    public void renderMenuBar() {
        for (Int2ObjectMap.Entry<OpenTexture> entry : this.openTextures.int2ObjectEntrySet()) {
            ImGui.menuItem(I18n.get("inspector.veil.texture.asset", entry.getIntKey()), null, entry.getValue().visible);
        }
    }

    @Override
    public boolean isMenuBarEnabled() {
        return !this.openTextures.isEmpty();
    }

    @Override
    public void onHide() {
        super.onHide();
        this.texturesSet.clear();
        this.cubemapStorage.values().forEach(CubemapStorage::free);
        this.cubemapStorage.clear();
        this.textures = new int[0];
        this.selectedTexture = 0;
    }

    private void addImage(int selectedId, boolean flipX, boolean flipY) {
        int target = VeilRenderSystem.directStateAccessSupported() ? glGetTextureParameteri(selectedId, GL_TEXTURE_TARGET) : GL_TEXTURE_2D;
        if (target == GL_TEXTURE_CUBE_MAP) {
            CubemapStorage storage = this.cubemapStorage.computeIfAbsent(selectedId, CubemapStorage::new);
            float size = ImGui.getContentRegionAvailX();

            storage.render((int) size, (int) (size / 2.0F));
            ImGui.image(storage.renderedTextureId(), size, size / 2.0F, flipX ? 1 : 0, flipY ? 1 : 0, flipX ? 0 : 1, flipY ? 0 : 1, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (target == GL_TEXTURE_2D) {
            CubemapStorage storage = this.cubemapStorage.remove(selectedId);
            if (storage != null) {
                storage.free();
            }

            RenderSystem.bindTexture(selectedId);
            int width = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);
            int height = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT);
            float size = ImGui.getContentRegionAvailX();
            ImGui.image(selectedId, size, size * (float) height / (float) width, flipX ? 1 : 0, flipY ? 1 : 0, flipX ? 0 : 1, flipY ? 0 : 1, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private record OpenTexture(ImBoolean open, ImBoolean visible, ImBoolean flipX, ImBoolean flipY) {

        private OpenTexture(boolean flipX, boolean flipY) {
            this(new ImBoolean(), new ImBoolean(true), new ImBoolean(flipX), new ImBoolean(flipY));
        }
    }

    private static final class CubemapStorage implements NativeResource {

        private final int texture;
        private AdvancedFbo fbo;

        private CubemapStorage(int texture) {
            this.texture = texture;
        }

        public void render(int width, int height) {
            ShaderProgram shaderProgram = VeilRenderSystem.setShader(DEBUG_CUBEMAP_SHADER);
            if (shaderProgram == null) {
                this.free();
                return;
            }

            if (this.fbo == null || this.fbo.getWidth() != width || this.fbo.getHeight() != height) {
                this.free();
                this.fbo = AdvancedFbo.withSize(width, height)
                        .addColorTextureBuffer()
                        .setDepthRenderBuffer()
                        .setDebugLabel("Texture Inspector Cubemap")
                        .build(true);
            }

            this.fbo.bind(true);
            this.fbo.clear();
            glBindTexture(GL_TEXTURE_CUBE_MAP, this.texture);
            shaderProgram.bind();
            VeilRenderSystem.drawScreenQuad();
            AdvancedFbo.unbind();
        }

        public int renderedTextureId() {
            return this.fbo.getColorTextureAttachment(0).getId();
        }

        @Override
        public void free() {
            if (this.fbo != null) {
                this.fbo.free();
                this.fbo = null;
            }
        }
    }
}
