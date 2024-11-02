package foundry.veil.api.client.render.framebuffer;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;

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
        glFramebufferTextureLayer(
                GL_FRAMEBUFFER,
                attachmentType + attachment,
                this.textureId,
                0,
                this.layer
        );
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

    public boolean setTexture(int textureId, int layer) {
        if (this.textureId == textureId && this.layer == layer) {
            return false;
        }

        this.textureId = textureId;
        this.layer = layer;
        return true;
    }
}
