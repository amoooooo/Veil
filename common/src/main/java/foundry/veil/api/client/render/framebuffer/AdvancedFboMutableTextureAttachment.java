package foundry.veil.api.client.render.framebuffer;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.texture.TextureFilter;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;

import static org.lwjgl.opengl.ARBDirectStateAccess.glNamedFramebufferTexture;
import static org.lwjgl.opengl.ARBDirectStateAccess.glNamedFramebufferTextureLayer;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glFramebufferTextureLayer;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30C.glFramebufferTexture2D;

@ApiStatus.Internal
public class AdvancedFboMutableTextureAttachment extends AdvancedFboTextureAttachment {

    private int textureId;
    private int layer;

    public AdvancedFboMutableTextureAttachment(int attachmentType, int textureId, int layer, String name) {
        super(attachmentType, 0, 0, 0, 0, 0, 1, TextureFilter.CLAMP, name);
        this.setTexture(textureId, layer);
    }

    @Override
    public void attach(int framebuffer, int attachment) {
        int attachmentType = this.getAttachmentType();
        Validate.isTrue(attachmentType < GL_DEPTH_ATTACHMENT || attachment == 0, "Only one depth buffer attachment is supported.");

        if (VeilRenderSystem.directStateAccessSupported()) {
            if (this.layer == -1) {
                glNamedFramebufferTexture(
                        framebuffer,
                        attachmentType + attachment,
                        this.textureId,
                        0
                );
            } else {
                glNamedFramebufferTextureLayer(
                        framebuffer,
                        attachmentType + attachment,
                        this.textureId,
                        0,
                        this.layer
                );
            }
        } else {
            if (this.layer == -1) {
                glFramebufferTexture2D(
                        GL_FRAMEBUFFER,
                        attachmentType + attachment,
                        GL_TEXTURE_2D,
                        this.textureId,
                        0
                );
            } else {
                glFramebufferTextureLayer(
                        GL_FRAMEBUFFER,
                        attachmentType + attachment,
                        this.textureId,
                        0,
                        this.layer
                );
            }
        }
    }

    @Override
    public void create() {
    }

    @Override
    public AdvancedFboMutableTextureAttachment clone() {
        return new AdvancedFboMutableTextureAttachment(this.getAttachmentType(), this.textureId, this.layer, this.getName());
    }

    @Override
    public void releaseId() {
    }

    @Override
    public int getId() {
        return this.textureId;
    }

    public void setTexture(int textureId, int layer) {
        this.textureId = textureId;
        this.layer = layer;
    }
}
