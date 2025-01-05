package foundry.veil.api.client.editor;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.imgui.VeilImGuiImpl;
import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.lwjgl.glfw.GLFW.glfwGetMonitorContentScale;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;

@ApiStatus.Internal
public class ImGuiFontManager implements PreparableReloadListener {

    private static final FileToIdConverter FONT_LISTER = new FileToIdConverter("font", ".ttf");
    private static final DecimalFormat FONT_FORMAT = new DecimalFormat("0.#");
    private static final float FONT_SIZE = 20.0f;

    private final Map<ResourceLocation, FontPackBuilder> fontBuilders;
    private final Map<ResourceLocation, FontPack> fonts;
    private ImFont defaultFont;

    public ImGuiFontManager() {
        this.fontBuilders = new HashMap<>();
        this.fonts = new HashMap<>();
    }

    public ImFont getFont(ResourceLocation name, boolean bold, boolean italic) {
        FontPack font = this.fonts.get(name);
        if (font == null) {
            return this.defaultFont;
        }

        if (italic ^ bold) {
            return italic ? font.italic : font.bold;
        } else {
            return italic ? font.boldItalic : font.regular;
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(@NotNull PreparationBarrier preparationBarrier, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller prepareProfiler, @NotNull ProfilerFiller applyProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor) {
        if (!VeilRenderSystem.hasImGui()) {
            return preparationBarrier.wait(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, FontData> fontData = new HashMap<>();
            for (Map.Entry<ResourceLocation, Resource> entry : FONT_LISTER.listMatchingResources(resourceManager).entrySet()) {
                ResourceLocation id = FONT_LISTER.fileToId(entry.getKey());
                Resource resource = entry.getValue();
                try (InputStream stream = resource.open()) {
                    short[] ranges = resource.metadata().getSection(ImGuiFontMetadataSectionSerializer.INSTANCE)
                            .map(ImGuiFontMetadataSectionSerializer.FontMetadata::ranges)
                            .orElseGet(() -> new short[]{0x0020, 0x00FF, 0});
                    fontData.put(id, new FontData(stream.readAllBytes(), ranges));
                } catch (IOException e) {
                    Veil.LOGGER.error("Failed to load ImGui font: {}", id, e);
                }
            }
            return fontData;
        }, backgroundExecutor).thenCompose(preparationBarrier::wait).thenAcceptAsync(fontData -> {
            this.fontBuilders.clear();
            for (Map.Entry<ResourceLocation, FontData> entry : fontData.entrySet()) {
                ResourceLocation id = entry.getKey();
                String[] parts = id.getPath().split("-", 2);
                if (parts.length < 2) {
                    continue;
                }

                ResourceLocation name = ResourceLocation.tryBuild(id.getNamespace(), parts[0]);
                if (name == null) {
                    Veil.LOGGER.error("Invalid font name: {}:{}", id.getNamespace(), parts[0]);
                    continue;
                }

                String type = parts[1];
                FontPackBuilder builder = this.fontBuilders.computeIfAbsent(name, FontPackBuilder::new);
                switch (type) {
                    case "regular" -> builder.main = entry.getValue();
                    case "italic" -> builder.italic = entry.getValue();
                    case "bold" -> builder.bold = entry.getValue();
                    case "bold_italic" -> builder.boldItalic = entry.getValue();
                    default -> Veil.LOGGER.warn("Unknown font type {} for font: {}", type, name);
                }
            }

            this.fontBuilders.entrySet().removeIf(entry -> {
                if (entry.getValue() == null) {
                    Veil.LOGGER.warn("Skipping invalid font: {}", entry.getKey());
                    return true;
                }
                return false;
            });

            Veil.LOGGER.info("Loaded {} ImGui fonts", this.fontBuilders.size());
            this.rebuildFonts();
        }, gameExecutor);
    }

    public void rebuildFonts() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Veil.beginImGui();
            ImFontAtlas atlas = ImGui.getIO().getFonts();
            atlas.clear();
            this.defaultFont = atlas.addFontDefault();
            this.fonts.clear();

            FloatBuffer xscale = stack.mallocFloat(1);
            FloatBuffer yscale = stack.mallocFloat(1);
            glfwGetMonitorContentScale(glfwGetPrimaryMonitor(), xscale, yscale);
            float scale = Math.max(xscale.get(0), yscale.get(0));

            for (Map.Entry<ResourceLocation, FontPackBuilder> entry : this.fontBuilders.entrySet()) {
                Veil.LOGGER.info("Built {}", entry.getKey());
                this.fonts.put(entry.getKey(), entry.getValue().build(FONT_SIZE * scale));
            }
            ImGui.getIO().setFontDefault(this.getFont(EditorManager.DEFAULT_FONT, false, false));
            VeilImGuiImpl.get().updateFonts();
        } finally {
            Veil.endImGui();
        }
    }

    private record FontPack(ImFont regular, ImFont italic, ImFont bold, ImFont boldItalic) implements NativeResource {

        @Override
        public void free() {
            this.regular.destroy();
            if (this.italic != this.regular) {
                this.italic.destroy();
            }
            if (this.bold != this.regular) {
                this.bold.destroy();
            }
            if (this.boldItalic != this.regular) {
                this.boldItalic.destroy();
            }
        }
    }

    private static class FontPackBuilder {

        private final ResourceLocation name;
        private FontData main;
        private FontData italic;
        private FontData bold;
        private FontData boldItalic;

        private FontPackBuilder(ResourceLocation name) {
            this.name = name;
        }

        private ImFont loadOrDefault(@Nullable FontData data, String type, float sizePixels, ImFont defaultFont) {
            if (data == null) {
                return defaultFont;
            } else {
                ImFontAtlas atlas = ImGui.getIO().getFonts();
                ImFontConfig fontConfig = new ImFontConfig();
                try {
                    fontConfig.setName(this.name.getPath() + " " + type + " " + FONT_FORMAT.format(sizePixels) + " px");
                    fontConfig.setGlyphRanges(data.ranges);
                    return atlas.addFontFromMemoryTTF(data.bytes, sizePixels, fontConfig);
                } finally {
                    fontConfig.destroy();
                }
            }
        }

        public FontPack build(float sizePixels) {
            ImFont main = Objects.requireNonNull(this.loadOrDefault(this.main, "regular", sizePixels, null));
            ImFont italic = this.loadOrDefault(this.italic, "italic", sizePixels, main);
            ImFont bold = this.loadOrDefault(this.bold, "bold", sizePixels, main);
            ImFont boldItalic = this.loadOrDefault(this.boldItalic, "bold_italic", sizePixels, main);
            return new FontPack(main, italic, bold, boldItalic);
        }
    }

    private record FontData(byte[] bytes, short[] ranges) {
    }
}
