package foundry.veil.api.quasar.emitters.module.init;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.InitParticleModule;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import foundry.veil.api.util.CodecUtil;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record InitRandomRotationModuleData(Vector3fc minDegrees, Vector3fc maxDegrees) implements ParticleModuleData {

    public static final MapCodec<InitRandomRotationModuleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CodecUtil.VECTOR3F_CODEC.fieldOf("min_degrees").forGetter(InitRandomRotationModuleData::minDegrees),
            CodecUtil.VECTOR3F_CODEC.fieldOf("max_degrees").forGetter(InitRandomRotationModuleData::maxDegrees)
    ).apply(instance, InitRandomRotationModuleData::new));

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule((InitParticleModule) particle -> {
            Vector3f rot = this.minDegrees.lerp(this.maxDegrees, particle.getRandomSource().nextFloat(), new Vector3f());
            particle.getRotation().add((float) Math.toRadians(rot.x), (float) Math.toRadians(rot.y), (float) Math.toRadians(rot.z));
        });
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.INIT_RANDOM_ROTATION;
    }
}
