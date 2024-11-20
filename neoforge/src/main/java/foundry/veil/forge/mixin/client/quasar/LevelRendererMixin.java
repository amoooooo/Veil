package foundry.veil.forge.mixin.client.quasar;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.client.render.CachedBufferSource;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Unique
    private CachedBufferSource veil$cachedBufferSource;

    @Inject(method = "setLevel", at = @At("HEAD"))
    public void setLevel(ClientLevel level, CallbackInfo ci) {
        VeilRenderSystem.renderer().getParticleManager().setLevel(level);
        if (this.veil$cachedBufferSource != null) {
            this.veil$cachedBufferSource.free();
            this.veil$cachedBufferSource = null;
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void close(CallbackInfo ci) {
        if (this.veil$cachedBufferSource != null) {
            this.veil$cachedBufferSource.free();
            this.veil$cachedBufferSource = null;
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", shift = At.Shift.AFTER))
    public void renderQuasarParticles(DeltaTracker pDeltaTracker, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix, CallbackInfo ci, @Local ProfilerFiller profiler, @Local PoseStack poseStack) {
        if (this.veil$cachedBufferSource == null) {
            this.veil$cachedBufferSource = new CachedBufferSource();
        }
        VeilRenderSystem.renderer().getParticleManager().render(VeilRenderBridge.create(poseStack), this.veil$cachedBufferSource, pCamera, VeilRenderer.getCullingFrustum(), pDeltaTracker.getRealtimeDeltaTicks());
        this.veil$cachedBufferSource.endBatch();
    }
}
