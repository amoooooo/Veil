package foundry.veil.api.quasar.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.util.CodecUtil;
import net.minecraft.core.Holder;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

public record EmitterSettings(List<Holder<EmitterShapeSettings>> emitterShapeSettingsHolders,
                              Holder<ParticleSettings> particleSettingsHolder,
                              boolean forceSpawn) {

    public static final Codec<EmitterSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtil.singleOrList(EmitterShapeSettings.CODEC).fieldOf("shape").forGetter(EmitterSettings::emitterShapeSettingsHolders),
            ParticleSettings.CODEC.fieldOf("particle_settings").forGetter(EmitterSettings::particleSettingsHolder),
            Codec.BOOL.optionalFieldOf("force_spawn", false).forGetter(EmitterSettings::forceSpawn)
    ).apply(instance, EmitterSettings::new));

    public List<EmitterShapeSettings> emitterShapeSettings() {
        return this.emitterShapeSettingsHolders.stream().map(Holder::value).toList();
    }

    public ParticleSettings particleSettings() {
        return this.particleSettingsHolder.value();
    }
}
