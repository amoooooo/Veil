package foundry.veil.mixin.client.quasar;

import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.quasar.particle.ParticleEmitter;
import foundry.veil.api.quasar.particle.ParticleSystemManager;
import foundry.veil.ext.EntityExtension;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Inject(method = "render", at = @At("HEAD"))
    public void render(T entity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, CallbackInfo ci) {
        // FIXME move to example mod
        if (!Veil.DEBUG) {
            return;
        }

        EntityExtension extension = (EntityExtension) entity;
        if (entity.isOnFire()) {
            if (extension.veil$getEmitters().isEmpty()) {
                ParticleSystemManager particleManager = VeilRenderSystem.renderer().getParticleManager();
                ParticleEmitter instance = particleManager.createEmitter(Veil.veilPath("basic_smoke"));
                if (instance == null) {
                    return;
                }

                instance.setAttachedEntity(entity);
//                instance.getEmitterSettingsModule().emissionShapeSettings().setDimensions(
//                        new Vector3f(
//                                pEntity.getBbWidth(),
//                                pEntity.getBbHeight(),
//                                pEntity.getBbWidth()
//                        )
//                );
//                instance.setLoop(true);
//                instance.setMaxLifetime(5);
//                instance.getParticleData().getForces().forEach(force -> {
//                    if (force instanceof PointForce pf) {
//                        pf.setPoint(pEntity::position);
//                    }
//                });
                // FIXME
                extension.veil$addEmitter(instance);
                particleManager.addParticleSystem(instance);
            } else {
//                ((EntityExtension) pEntity).getEmitters().stream().filter(emitter -> emitter.registryName.toString().equals("veil:basic_smoke")).forEach(emitter -> emitter.getEmitterModule().setMaxLifetime(5));
            }
        } else {
//            ((EntityExtension) pEntity).getEmitters().stream().filter(emitter -> emitter.registryName.toString().equals("veil:basic_smoke")).forEach(p -> p.getEmitterModule().setLoop(false));
            extension.veil$getEmitters().forEach(emitter -> {
                if ("veil:basic_smoke".equals(String.valueOf(emitter.getRegistryName()))) {
                    emitter.remove();
                }
            });
            extension.veil$getEmitters().removeIf(emitter -> "veil:basic_smoke".equals(String.valueOf(emitter.getRegistryName())));
        }
    }
}
