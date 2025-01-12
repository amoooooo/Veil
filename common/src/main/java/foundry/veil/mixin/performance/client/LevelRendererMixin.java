package foundry.veil.mixin.performance.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import foundry.veil.ext.PerformanceRenderTargetExtension;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;clear(Z)V", ordinal = 0))
    public void clearItemEntityDepth(RenderTarget instance, boolean clearError) {
        ((PerformanceRenderTargetExtension) instance).veil$clearColorBuffer(clearError);
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;clear(Z)V", ordinal = 3))
    public void clearTranslucentDepth(RenderTarget instance, boolean clearError) {
        ((PerformanceRenderTargetExtension) instance).veil$clearColorBuffer(clearError);
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;clear(Z)V", ordinal = 4))
    public void clearParticlesDepth(RenderTarget instance, boolean clearError) {
        ((PerformanceRenderTargetExtension) instance).veil$clearColorBuffer(clearError);
    }
}
