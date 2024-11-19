package foundry.veil.api.quasar.data.module.init;

import com.mojang.serialization.MapCodec;
import foundry.veil.api.molang.MolangExpressionCodec;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.InitParticleModule;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.exception.MolangRuntimeException;

public record InitSizeParticleModuleData(MolangExpression size) implements ParticleModuleData {

    public static final MapCodec<InitSizeParticleModuleData> CODEC = MolangExpressionCodec.CODEC.fieldOf("size").xmap(InitSizeParticleModuleData::new, InitSizeParticleModuleData::size);

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule((InitParticleModule) particle -> {
            try {
                particle.setRadius(particle.getEnvironment().resolve(this.size));
            } catch (MolangRuntimeException e) {
                e.printStackTrace();
                particle.setRadius(1.0F);
            }
        });
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.INIT_SIZE;
    }
}
