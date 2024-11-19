package foundry.veil.api.quasar.data.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

@FunctionalInterface
public interface ModuleType<T extends ParticleModuleData> {
    /**
     * @return The codec for this module type data
     */
    MapCodec<T> codec();
}
