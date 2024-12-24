package foundry.veil.ext;

import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import org.jetbrains.annotations.Nullable;

public interface RenderTargetExtension {

    void veil$setWrapper(@Nullable AdvancedFbo fbo);

    void veil$bindDrawFramebuffer();

    void veil$bindReadFramebuffer();
}
