package foundry.veil.api.quasar.data.module.force;

import com.mojang.serialization.Codec;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.force.ScaleForceModule;
import foundry.veil.api.quasar.particle.ParticleModuleSet;

/**
 * A force that applies a drag force to a particle.
 *
 * <p>
 * Drag forces are forces that are applied in the opposite direction of the particle's velocity.
 * They are useful for simulating air resistance.
 * The strength of the force is determined by the strength parameter.
 */
public record DragForceData(double strength) implements ParticleModuleData {

    public static final Codec<DragForceData> CODEC = Codec.DOUBLE.fieldOf("strength").xmap(DragForceData::new, DragForceData::strength).codec();

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule(new ScaleForceModule(this.strength));
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.DRAG;
    }
}
