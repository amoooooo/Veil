package foundry.veil.api.quasar.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * @param maxLifetime
 * @param loop               Whether the emitter will loop. If <code>true</code>, the emitter will reset after maxLifetime ticks
 * @param rate               The rate at which particles are emitted. Count particles per rate ticks.
 * @param count              The number of particles emitted per rate ticks
 * @param maxParticles       The maximum number of particles to have alive
 * @param emitterSettings    The settings for how to emit particles
 * @param particleDataHolder The particle to emit
 */
public record ParticleEmitterData(int maxLifetime,
                                  boolean loop,
                                  int rate,
                                  int count,
                                  int maxParticles,
                                  EmitterSettings emitterSettings,
                                  Holder<QuasarParticleData> particleDataHolder) {

    public static final Codec<ParticleEmitterData> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("max_lifetime").forGetter(ParticleEmitterData::maxLifetime),
            Codec.BOOL.optionalFieldOf("loop", false).forGetter(ParticleEmitterData::loop),
            Codec.INT.fieldOf("rate").forGetter(ParticleEmitterData::rate),
            Codec.INT.fieldOf("count").forGetter(ParticleEmitterData::count),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("count", Integer.MAX_VALUE).forGetter(ParticleEmitterData::maxParticles),
            EmitterSettings.CODEC.fieldOf("emitter_settings").forGetter(ParticleEmitterData::emitterSettings),
            QuasarParticleData.CODEC.fieldOf("particle_data").forGetter(ParticleEmitterData::particleDataHolder)
    ).apply(instance, ParticleEmitterData::new));
    public static final Codec<Holder<ParticleEmitterData>> CODEC = RegistryFileCodec.create(QuasarParticles.EMITTER, DIRECT_CODEC);

    public QuasarParticleData particleData() {
        return this.particleDataHolder.value();
    }

    public @Nullable ResourceLocation getRegistryId() {
        return this.particleData().getRegistryId();
    }
}
