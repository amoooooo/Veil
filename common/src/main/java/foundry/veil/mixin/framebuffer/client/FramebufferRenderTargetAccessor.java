package foundry.veil.mixin.framebuffer.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderTarget.class)
public interface FramebufferRenderTargetAccessor {

    @Accessor
    float[] getClearChannels();
}
