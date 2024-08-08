package foundry.veil.api.quasar.data.module.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.InitParticleModule;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import foundry.veil.api.util.CodecUtil;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public record InitialVelocityModuleData(Vector3dc velocityDirection,
                                        boolean takesParentRotation,
                                        float strength) implements ParticleModuleData {

    public static final Codec<InitialVelocityModuleData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.VECTOR3D_CODEC.fieldOf("direction").forGetter(InitialVelocityModuleData::velocityDirection),
                    Codec.BOOL.fieldOf("take_parent_rotation").orElse(true).forGetter(InitialVelocityModuleData::takesParentRotation),
                    Codec.FLOAT.fieldOf("strength").forGetter(InitialVelocityModuleData::strength)
            ).apply(instance, InitialVelocityModuleData::new));

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        // TODO takesParentRotation
        builder.addModule((InitParticleModule) particle -> particle.getVelocity().add(this.velocityDirection.normalize(this.strength, new Vector3d())));
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.INITIAL_VELOCITY;
    }
}
