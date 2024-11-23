package foundry.veil.impl.client.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.editor.SingleWindowEditor;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.api.client.util.TextureDownloader;
import imgui.ImGui;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11C.*;

@ApiStatus.Internal
public class FramebufferEditor extends SingleWindowEditor {

    public static final Component TITLE = Component.translatable("editor.veil.framebuffer.title");

    private static final Component SAVE = Component.translatable("gui.veil.save");

    private final Set<ResourceLocation> framebuffers;
    private AdvancedFbo downloadBuffer;

    public FramebufferEditor() {
        this.framebuffers = new TreeSet<>();
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
            this.framebuffers.clear();
            this.framebuffers.addAll(renderer.getFramebufferManager().getFramebuffers().keySet());
            for (ResourceLocation id : this.framebuffers) {
                drawBuffers(id, fbo -> this.downloadBuffer = fbo);
            }
            ImGui.endTabBar();
        }
    }

    @Override
    public void renderLast() {
        super.renderLast();

        if (this.downloadBuffer != null) {
            try {
                Minecraft client = Minecraft.getInstance();
                Path outputFolder = Paths.get(client.gameDirectory.toURI()).resolve("debug-out").resolve("deferred");
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
                for (int i = 0; i < this.downloadBuffer.getColorAttachments(); i++) {
                    if (this.downloadBuffer.isColorTextureAttachment(i)) {
                        AdvancedFboTextureAttachment attachment = this.downloadBuffer.getColorTextureAttachment(i);
                        String name = attachment.getName() != null ? attachment.getName() : "Attachment " + i;
                        result.add(TextureDownloader.save(name, outputFolder, attachment.getId(), true));
                    }
                }

                if (this.downloadBuffer.isDepthTextureAttachment()) {
                    AdvancedFboTextureAttachment attachment = this.downloadBuffer.getDepthTextureAttachment();
                    String name = attachment.getName() != null ? attachment.getName() : "Depth Attachment";
                    result.add(TextureDownloader.save(name, outputFolder, attachment.getId(), true));
                }

                CompletableFuture.allOf(result.toArray(new CompletableFuture[0])).thenRunAsync(() -> Util.getPlatform().openFile(outputFolder.toFile()), client);
            } catch (Exception e) {
                Veil.LOGGER.error("Failed to download framebuffer", e);
            }
            this.downloadBuffer = null;
        }
    }

    public static void drawBuffers(ResourceLocation id, @Nullable Consumer<AdvancedFbo> saveCallback) {
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
                    ImGui.text(getAttachmentName(i, attachment));
                    ImGui.image(attachment.getId(), width, height, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.5F);
                    ImGui.endGroup();
                }

                if (buffer.isDepthTextureAttachment()) {
                    if (i % columns != 0) {
                        ImGui.sameLine();
                    }
                    ImGui.beginGroup();
                    AdvancedFboTextureAttachment attachment = buffer.getDepthTextureAttachment();
                    ImGui.text(getAttachmentName(-1, attachment));
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

    private static String getAttachmentName(int index, AdvancedFboTextureAttachment attachment) {
        RenderSystem.bindTexture(attachment.getId());
        StringBuilder attachmentName = new StringBuilder(attachment.getName() != null ? attachment.getName() : index == -1 ? I18n.get("editor.veil.framebuffer.depth_attachment") : (I18n.get("editor.veil.framebuffer.color_attachment", index)));

        int internalFormat = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_INTERNAL_FORMAT);
        for (FramebufferAttachmentDefinition.Format format : FramebufferAttachmentDefinition.Format.values()) {
            if (internalFormat == format.getInternalId()) {
                attachmentName.append(" (").append(format.name()).append(")");
                return attachmentName.toString();
            }
        }

        attachmentName.append(" (0x%X)".formatted(internalFormat));
        return attachmentName.toString();
    }
}
