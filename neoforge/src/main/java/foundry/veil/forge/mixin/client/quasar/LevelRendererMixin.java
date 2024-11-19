package foundry.veil.forge.mixin.client.quasar;

import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.client.render.CachedBufferSource;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
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

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", shift = At.Shift.AFTER))
    public void renderQuasarParticles(PoseStack poseStack, float partialTicks, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci) {
        if (this.veil$cachedBufferSource == null) {
            this.veil$cachedBufferSource = new CachedBufferSource();
        }
        VeilRenderSystem.renderer().getParticleManager().render(poseStack, this.veil$cachedBufferSource, camera, VeilRenderer.getCullingFrustum(), partialTicks);
        this.veil$cachedBufferSource.endBatch();
    }
}
