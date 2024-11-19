package foundry.veil.fabric.mixin.client.deferred;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.render.deferred.DeferredShaderStateCache;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
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

    @Inject(method = "render", at = @At("HEAD"))
    public void setupRenderState(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        VeilRenderSystem.renderer().getDeferredRenderer().setup();
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void clearRenderState(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        VeilRenderSystem.renderer().getDeferredRenderer().clear();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"), index = 0, remap = false)
    public Supplier<ShaderInstance> setShader(Supplier<ShaderInstance> value) {
        return () -> this.veil$cache.getShader(value.get());
    }
}
