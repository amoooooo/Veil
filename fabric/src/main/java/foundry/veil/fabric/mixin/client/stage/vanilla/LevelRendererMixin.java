package foundry.veil.fabric.mixin.client.stage.vanilla;

import foundry.veil.fabric.ext.LevelRendererExtension;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderSectionLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;clearRenderState()V", shift = At.Shift.BEFORE))
    public void postRenderSectionLayer(RenderType renderType, double x, double y, double z, Matrix4f frustrumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        ((LevelRendererExtension) this).veil$renderStage(renderType, frustrumMatrix, projectionMatrix);
    }
}
