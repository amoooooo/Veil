package foundry.veil.api.quasar.data.module.init;

import com.mojang.serialization.Codec;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.InitParticleModule;
import foundry.veil.api.quasar.particle.ParticleEmitter;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import foundry.veil.api.quasar.particle.ParticleSystemManager;
import net.minecraft.resources.ResourceLocation;

public record InitSubEmitterModuleData(ResourceLocation subEmitter) implements ParticleModuleData {

    public static final Codec<InitSubEmitterModuleData> CODEC = ResourceLocation.CODEC.fieldOf("subemitter").xmap(InitSubEmitterModuleData::new, InitSubEmitterModuleData::subEmitter).codec();

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule((InitParticleModule) (particle -> {
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
        return ParticleModuleTypeRegistry.INIT_SUB_EMITTER;
    }
}
