package foundry.veil.mixin.client.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = {"net.minecraft.client.particle.ParticleRenderType$1", "net.minecraft.client.particle.ParticleRenderType$3"})
public class ParticleRenderTypeMixin {

    /**
     * This corrects the blend function for particles. This also fixes particles in Fabulous graphics
     *
     * @author Ocelot
     */
    @Redirect(method = "begin", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;defaultBlendFunc()V"))
    public void changeBlendFunction() {
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }
}
