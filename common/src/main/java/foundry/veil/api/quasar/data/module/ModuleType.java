package foundry.veil.api.quasar.data.module;

import com.mojang.serialization.Codec;

@FunctionalInterface
public interface ModuleType<T extends ParticleModuleData> {
    /**
     * @return The codec for this module type data
     */
    Codec<T> codec();
}
