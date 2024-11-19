package foundry.veil.api.quasar.data.module.update;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.update.TickSubEmitterModule;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import net.minecraft.resources.ResourceLocation;

public record TickSubEmitterModuleData(ResourceLocation subEmitter, int frequency) implements ParticleModuleData {

    public static final MapCodec<TickSubEmitterModuleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("subemitter").forGetter(TickSubEmitterModuleData::subEmitter),
            Codec.INT.fieldOf("frequency").forGetter(TickSubEmitterModuleData::frequency)
    ).apply(instance, TickSubEmitterModuleData::new));

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule(new TickSubEmitterModule(this));
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.TICK_SUB_EMITTER;
    }
}
