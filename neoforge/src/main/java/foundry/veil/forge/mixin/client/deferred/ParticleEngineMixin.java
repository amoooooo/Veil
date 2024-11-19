package foundry.veil.forge.mixin.client.deferred;

import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.render.deferred.DeferredShaderStateCache;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Unique
    private final DeferredShaderStateCache veil$cache = new DeferredShaderStateCache();

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", at = @At("HEAD"), remap = false)
    public void setupRenderState(PoseStack p_107337_, MultiBufferSource.BufferSource p_107338_, LightTexture p_107339_, Camera p_107340_, float p_107341_, Frustum clippingHelper, CallbackInfo ci) {
        VeilRenderSystem.renderer().getDeferredRenderer().setup();
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", at = @At("RETURN"), remap = false)
    public void clearRenderState(PoseStack p_107337_, MultiBufferSource.BufferSource p_107338_, LightTexture p_107339_, Camera p_107340_, float p_107341_, Frustum clippingHelper, CallbackInfo ci) {
        VeilRenderSystem.renderer().getDeferredRenderer().clear();
    }

    @ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"), index = 0, remap = false)
    public Supplier<ShaderInstance> setShader(Supplier<ShaderInstance> value) {
        return () -> this.veil$cache.getShader(value.get());
    }
}
