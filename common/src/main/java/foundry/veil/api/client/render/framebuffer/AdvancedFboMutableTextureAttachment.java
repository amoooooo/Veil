package foundry.veil.api.client.render.framebuffer;

import com.mojang.blaze3d.platform.GlStateManager;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glFramebufferTextureLayer;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;

@ApiStatus.Internal
public class AdvancedFboMutableTextureAttachment extends AdvancedFboTextureAttachment {

    private int textureId;
    private int layer;

    public AdvancedFboMutableTextureAttachment(int attachmentType, int textureId, int layer) {
        super(attachmentType, 0, 0, 0, 0, 0, 0, false, null);
        this.setTexture(textureId, layer);
    }

    @Override
    public void attach(int attachment) {
        int attachmentType = this.getAttachmentType();
        Validate.isTrue(attachmentType < GL_DEPTH_ATTACHMENT || attachment == 0, "Only one depth buffer attachment is supported.");

        if (this.layer == -1) {
            GlStateManager._glFramebufferTexture2D(
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

    @Override
    public void create() {
    }

    @Override
    public AdvancedFboMutableTextureAttachment clone() {
        return new AdvancedFboMutableTextureAttachment(this.textureId, this.getAttachmentType(), this.layer);
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
