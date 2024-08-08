package foundry.veil.api.quasar.data.module.collision;

import com.mojang.serialization.Codec;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.CollisionParticleModule;
import foundry.veil.api.quasar.particle.ParticleEmitter;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import foundry.veil.api.quasar.particle.ParticleSystemManager;
import net.minecraft.resources.ResourceLocation;

public record CollisionSubEmitterData(ResourceLocation subEmitter) implements ParticleModuleData {

    public static final Codec<CollisionSubEmitterData> CODEC = ResourceLocation.CODEC.fieldOf("subemitter").xmap(CollisionSubEmitterData::new, CollisionSubEmitterData::subEmitter).codec();

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule((CollisionParticleModule) (particle -> {
            ParticleSystemManager particleManager = VeilRenderSystem.renderer().getParticleManager();
            ParticleEmitter instance = particleManager.createEmitter(this.subEmitter);
            if (instance == null) {
                return;
            }

            instance.setPosition(particle.getPosition());
            particleManager.addParticleSystem(instance);
        }));
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.SUB_EMITTER_COLLISION;
    }
}
