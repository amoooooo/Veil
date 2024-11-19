package foundry.veil.api.quasar.data.module.force;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.force.PointAttractorForceModule;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import foundry.veil.api.util.CodecUtil;
import org.joml.Vector3dc;

/**
 * A force that attracts particles to a point.
 *
 * <p>
 * Point attractor forces are forces that are applied in the direction of a point.
 * They are useful for simulating gravity or other forces that pull particles towards a point.
 * The strength of the force is determined by the strength parameter.
 * The falloff parameter determines how quickly the force falls off with distance. (unused)
 * The strengthByDistance parameter determines whether the strength of the force is multiplied by the distance from the point.
 * If strengthByDistance is true, the strength of the force is multiplied by (1 - distance / range).
 * If strengthByDistance is false, the strength of the force is not affected by distance.
 * The range parameter determines the maximum distance from the point at which the force is applied.
 * If the distance from the point is greater than the range, the force is not applied.
 * The position parameter determines the position of the point.
 * The position parameter can be a Vec3 or a Supplier Vec3.
 * If the position parameter is a Vec3, the position of the point is fixed.
 * If the position parameter is a Supplier Vec3, the position of the point is updated every tick.
 * This allows the point to move.
 * </p>
 */
public record PointAttractorForceData(Vector3dc position,
                                      boolean localPosition,
                                      float range,
                                      float strength,
                                      boolean strengthByDistance,
                                      boolean invertDistanceModifier) implements ParticleModuleData {

    public static final MapCodec<PointAttractorForceData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CodecUtil.VECTOR3D_CODEC.fieldOf("position").forGetter(PointAttractorForceData::position),
            Codec.BOOL.optionalFieldOf("localPosition", false).forGetter(PointAttractorForceData::invertDistanceModifier),
            Codec.FLOAT.fieldOf("range").forGetter(PointAttractorForceData::range),
            Codec.FLOAT.fieldOf("strength").forGetter(PointAttractorForceData::strength),
            Codec.BOOL.fieldOf("strengthByDistance").forGetter(PointAttractorForceData::strengthByDistance),
            Codec.BOOL.optionalFieldOf("invertDistanceModifier", false).forGetter(PointAttractorForceData::invertDistanceModifier)
    ).apply(instance, PointAttractorForceData::new));

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule(new PointAttractorForceModule(this));
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.POINT_ATTRACTOR;
    }
}
