package foundry.veil.mixin.client.quasar;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.quasar.particle.ParticleSystemManager;
import foundry.veil.impl.quasar.QuasarParticleHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "countParticles", at = @At("RETURN"), cancellable = true)
    public void countParticles(CallbackInfoReturnable<String> cir) {
        ParticleSystemManager particleManager = VeilRenderSystem.renderer().getParticleManager();
        cir.setReturnValue(cir.getReturnValue() + ". VE: " + particleManager.getEmitterCount() + ". VP: " + particleManager.getParticleCount());
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    public void setLevel(@Nullable ClientLevel level, CallbackInfo ci) {
        QuasarParticleHandler.setLevel(level);
    }

    @Inject(method = "clearParticles", at = @At("TAIL"))
    public void clear(CallbackInfo ci) {
        QuasarParticleHandler.free();
    }
}
