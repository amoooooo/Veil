package foundry.veil.api.quasar.data.module.force;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.force.VectorFieldForceModule;
import foundry.veil.api.quasar.emitters.module.update.VectorField;
import foundry.veil.api.quasar.particle.ParticleModuleSet;

/**
 * <p>A force that applies the force created in a vector field to a particle.</p>
 * <p>Vector fields are useful for creating complex forces that vary over time.</p>
 *
 * @see VectorField
 */
public record VectorFieldForceData(VectorField vectorField,
                                   float strength) implements ParticleModuleData {

    public static final Codec<VectorFieldForceData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VectorField.CODEC.fieldOf("vector_field").forGetter(VectorFieldForceData::vectorField),
            Codec.FLOAT.fieldOf("strength").forGetter(VectorFieldForceData::strength)
    ).apply(instance, VectorFieldForceData::new));

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule(new VectorFieldForceModule(this));
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.VECTOR_FIELD;
    }
}
