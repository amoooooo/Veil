package foundry.veil.impl.client.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.editor.SingleWindowInspector;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.api.client.util.TextureDownloader;
import foundry.veil.api.compat.IrisCompat;
import foundry.veil.ext.iris.IrisRenderTargetExtension;
import imgui.ImGui;
import imgui.type.ImBoolean;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11C.*;

@ApiStatus.Internal
public class FramebufferInspector extends SingleWindowInspector {

    public static final Component TITLE = Component.translatable("inspector.veil.framebuffer.title");

    private static final Component SAVE = Component.translatable("gui.veil.save");
    private static final Component SHOW_ALT = Component.translatable("inspector.veil.framebuffer.iris.show_alt");
    private static final Component SHOW_ALT_TOOLTIP = Component.translatable("inspector.veil.framebuffer.iris.show_alt.desc");

    private final Set<ResourceLocation> framebuffers;
    private final ImBoolean showAlt;
    private AdvancedFbo downloadFramebuffer;
    private IrisRenderTargetExtension downloadRenderTarget;

    public FramebufferInspector() {
        this.framebuffers = new TreeSet<>();
        this.showAlt = new ImBoolean();
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
        VeilRenderer renderer = VeilRenderSystem.renderer();

        if (ImGui.beginTabBar("##framebuffers")) {
            // Sort ids
            this.framebuffers.addAll(renderer.getFramebufferManager().getFramebuffers().keySet());
            for (ResourceLocation id : this.framebuffers) {
                this.drawBuffers(id, fbo -> this.downloadFramebuffer = fbo);
            }
            if (IrisCompat.INSTANCE != null) {
                Map<String, IrisRenderTargetExtension> renderTargets = IrisCompat.INSTANCE.getRenderTargets();

                this.framebuffers.clear();
                for (String name : renderTargets.keySet()) {
                    this.framebuffers.add(ResourceLocation.fromNamespaceAndPath("iris", name));
                }
                for (ResourceLocation id : this.framebuffers) {
                    this.drawRenderTarget(id, renderTargets.get(id.getPath()), renderTarget -> this.downloadRenderTarget = renderTarget);
                }
            }
            this.framebuffers.clear();
            ImGui.endTabBar();
        }
    }

    @Override
    public void renderLast() {
        super.renderLast();

        if (this.downloadFramebuffer != null) {
            try {
                Minecraft client = Minecraft.getInstance();
                Path outputFolder = Paths.get(client.gameDirectory.toURI()).resolve("debug-out").resolve(Veil.MODID + "-framebuffer");
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
                for (int i = 0; i < this.downloadFramebuffer.getColorAttachments(); i++) {
                    if (this.downloadFramebuffer.isColorTextureAttachment(i)) {
                        AdvancedFboTextureAttachment attachment = this.downloadFramebuffer.getColorTextureAttachment(i);
                        String name = attachment.getName() != null ? attachment.getName() : "Attachment " + i;
                        result.add(TextureDownloader.save(name, outputFolder, attachment.getId(), true));
                    }
                }

                if (this.downloadFramebuffer.isDepthTextureAttachment()) {
                    AdvancedFboTextureAttachment attachment = this.downloadFramebuffer.getDepthTextureAttachment();
                    String name = attachment.getName() != null ? attachment.getName() : "Depth Attachment";
                    result.add(TextureDownloader.save(name, outputFolder, attachment.getId(), true));
                }

                CompletableFuture.allOf(result.toArray(new CompletableFuture[0])).thenRunAsync(() -> Util.getPlatform().openFile(outputFolder.toFile()), client);
            } catch (Exception e) {
                Veil.LOGGER.error("Failed to download framebuffer", e);
            }
            this.downloadFramebuffer = null;
        }
        if (this.downloadRenderTarget != null) {
            try {
                Minecraft client = Minecraft.getInstance();
                Path outputFolder = Paths.get(client.gameDirectory.toURI()).resolve("debug-out").resolve("iris-rendertarget");
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

                String name = this.downloadRenderTarget.veil$getName();
                CompletableFuture.allOf(
                        TextureDownloader.save(name + " Main", outputFolder, this.downloadRenderTarget.veil$getMainTexture(), true),
                        TextureDownloader.save(name + " Alt", outputFolder, this.downloadRenderTarget.veil$getAltTexture(), true)
                ).thenRunAsync(() -> Util.getPlatform().openFile(outputFolder.toFile()), client);
            } catch (Exception e) {
                Veil.LOGGER.error("Failed to download iris render target", e);
            }
            this.downloadRenderTarget = null;
        }
    }

    private void drawBuffers(ResourceLocation id, @Nullable Consumer<AdvancedFbo> saveCallback) {
        AdvancedFbo buffer = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(id);
        ImGui.beginDisabled(buffer == null);
        if (ImGui.beginTabItem(id.toString())) {
            if (buffer != null) {
                int columns = (int) Math.ceil(Math.sqrt(buffer.getColorAttachments() + (buffer.isDepthTextureAttachment() ? 1 : 0)));
                float width = ImGui.getContentRegionAvailX() / columns - ImGui.getStyle().getItemSpacingX();
                float height = width * buffer.getHeight() / buffer.getWidth();
                int i;
                for (i = 0; i < buffer.getColorAttachments(); i++) {
                    if (!buffer.isColorTextureAttachment(i)) {
                        continue;
                    }

                    if (i % columns != 0) {
                        ImGui.sameLine();
                    }
                    ImGui.beginGroup();
                    AdvancedFboTextureAttachment attachment = buffer.getColorTextureAttachment(i);
                    ImGui.text(getAttachmentName(i, attachment.getId(), attachment.getName()));
                    ImGui.image(attachment.getId(), width, height, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.5F);
                    ImGui.endGroup();
                }

                if (buffer.isDepthTextureAttachment()) {
                    if (i % columns != 0) {
                        ImGui.sameLine();
                    }
                    ImGui.beginGroup();
                    AdvancedFboTextureAttachment attachment = buffer.getDepthTextureAttachment();
                    ImGui.text(getAttachmentName(-1, attachment.getId(), attachment.getName()));
                    ImGui.image(attachment.getId(), width, height, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.5F);
                    ImGui.endGroup();
                }

                if (saveCallback != null && ImGui.button(SAVE.getString(), ImGui.getContentRegionAvailX() - 4, 0)) {
                    saveCallback.accept(buffer);
                }
            }
            ImGui.endTabItem();
        }
        ImGui.endDisabled();
    }

    private void drawRenderTarget(ResourceLocation id, @Nullable IrisRenderTargetExtension renderTarget, @Nullable Consumer<IrisRenderTargetExtension> saveCallback) {
        ImGui.beginDisabled(renderTarget == null);
        if (ImGui.beginTabItem(id.toString())) {
            ImGui.checkbox(SHOW_ALT.getString(), this.showAlt);
            if (ImGui.isItemHovered()) {
                VeilImGuiUtil.setTooltip(SHOW_ALT_TOOLTIP);
            }
            if (renderTarget != null) {
                float width = ImGui.getContentRegionAvailX() - ImGui.getStyle().getItemSpacingX();
                float height = width * renderTarget.veil$getHeight() / renderTarget.veil$getWidth();

                int texture = this.showAlt.get() ? renderTarget.veil$getAltTexture() : renderTarget.veil$getMainTexture();
                ImGui.beginGroup();
                ImGui.text(getAttachmentName(0, texture, this.showAlt.get() ? "Alt" : "Main"));
                ImGui.image(texture, width, height, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.5F);
                ImGui.endGroup();

                if (saveCallback != null && ImGui.button(SAVE.getString(), ImGui.getContentRegionAvailX() - 4, 0)) {
                    saveCallback.accept(renderTarget);
                }
            }
            ImGui.endTabItem();
        }
        ImGui.endDisabled();
    }

    private static String getAttachmentName(int index, int id, @Nullable String name) {
        RenderSystem.bindTexture(id);
        StringBuilder attachmentName = new StringBuilder(name != null ? name : index == -1 ? I18n.get("inspector.veil.framebuffer.depth_attachment") : (I18n.get("inspector.veil.framebuffer.color_attachment", index)));

        int internalFormat = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_INTERNAL_FORMAT);
        for (FramebufferAttachmentDefinition.Format format : FramebufferAttachmentDefinition.Format.values()) {
            if (internalFormat == format.getInternalFormat()) {
                attachmentName.append(" (").append(format.name()).append(")");
                return attachmentName.toString();
            }
        }

        attachmentName.append(" (0x%X)".formatted(internalFormat));
        return attachmentName.toString();
    }
}
