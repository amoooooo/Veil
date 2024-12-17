package foundry.veil.ext.iris;

import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;

public interface IrisRenderTargetExtension {

    String veil$getName();

    int veil$getMainTexture();

    int veil$getAltTexture();

    int veil$getWidth();

    int veil$getHeight();

    FramebufferAttachmentDefinition.Format veil$getFormat();

    FramebufferAttachmentDefinition.DataType veil$getDataType();
}
