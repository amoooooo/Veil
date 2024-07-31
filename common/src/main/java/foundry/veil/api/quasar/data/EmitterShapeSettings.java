package foundry.veil.api.quasar.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.quasar.emitters.shape.EmitterShape;
import foundry.veil.api.util.CodecUtil;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

public record EmitterShapeSettings(EmitterShape shape,
                                   Vector3fc dimensions,
                                   Vector3fc rotation,
                                   boolean fromSurface) {

    public static final Codec<EmitterShapeSettings> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EmitterShape.CODEC.fieldOf("shape").forGetter(EmitterShapeSettings::shape),
            CodecUtil.VECTOR3F_CODEC.fieldOf("dimensions").forGetter(EmitterShapeSettings::dimensions),
            CodecUtil.VECTOR3F_CODEC.fieldOf("rotation").forGetter(EmitterShapeSettings::rotation),
            Codec.BOOL.fieldOf("from_surface").forGetter(EmitterShapeSettings::fromSurface)
    ).apply(instance, EmitterShapeSettings::new));
    public static final Codec<Holder<EmitterShapeSettings>> CODEC = RegistryFileCodec.create(QuasarParticles.EMITTER_SHAPE_SETTINGS, DIRECT_CODEC);

    public Vector3d getPos(RandomSource randomSource, Vector3dc pos) {
        return this.shape.getPoint(randomSource, this.dimensions, this.rotation, pos, this.fromSurface);
    }

    public @Nullable ResourceLocation getRegistryId() {
        return QuasarParticles.registryAccess().registry(QuasarParticles.EMITTER_SHAPE_SETTINGS).map(registry -> registry.getKey(this)).orElse(null);
    }
}
