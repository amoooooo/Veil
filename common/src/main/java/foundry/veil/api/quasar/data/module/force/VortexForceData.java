package foundry.veil.api.quasar.data.module.force;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.force.VortexForceModule;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import foundry.veil.api.util.CodecUtil;
import org.joml.Vector3dc;

/**
 * A force that applies a vortex force to a particle.
 *
 * <p>
 * Vortex forces are forces that are applied in a circular motion around a center point.
 * They are useful for simulating whirlpools or tornadoes.
 * The strength of the force is determined by the strength parameter.
 * The falloff parameter determines how quickly the force falls off with distance. (unused)
 */
public record VortexForceData(Vector3dc vortexAxis,
                              Vector3dc vortexCenter,
                              boolean localPosition,
                              double range,
                              float strength) implements ParticleModuleData {

    public static final Codec<VortexForceData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtil.VECTOR3D_CODEC.fieldOf("vortex_axis").forGetter(VortexForceData::vortexAxis),
            CodecUtil.VECTOR3D_CODEC.fieldOf("vortex_center").forGetter(VortexForceData::vortexCenter),
            Codec.BOOL.optionalFieldOf("local_position", false).forGetter(VortexForceData::localPosition),
            Codec.DOUBLE.fieldOf("range").forGetter(VortexForceData::range),
            Codec.FLOAT.fieldOf("strength").forGetter(VortexForceData::strength)
    ).apply(instance, VortexForceData::new));

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule(new VortexForceModule(this));
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.VORTEX;
    }
}
