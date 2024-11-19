package foundry.veil.api.quasar.data.module.update;

import com.mojang.serialization.MapCodec;
import foundry.veil.api.molang.MolangExpressionCodec;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.UpdateParticleModule;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.exception.MolangRuntimeException;

public record TickSizeParticleModuleData(MolangExpression size) implements ParticleModuleData {

    public static final MapCodec<TickSizeParticleModuleData> CODEC = MolangExpressionCodec.CODEC.fieldOf("size").xmap(TickSizeParticleModuleData::new, TickSizeParticleModuleData::size);

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule((UpdateParticleModule) particle -> {
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
        return ParticleModuleTypeRegistry.TICK_SIZE;
    }
}
