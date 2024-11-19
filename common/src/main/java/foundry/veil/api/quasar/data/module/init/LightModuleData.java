package foundry.veil.api.quasar.data.module.init;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.molang.MolangExpressionCodec;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.render.DynamicLightModule;
import foundry.veil.api.quasar.emitters.module.render.StaticLightModule;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import foundry.veil.impl.quasar.ColorGradient;
import gg.moonflower.molangcompiler.api.MolangExpression;

public record LightModuleData(ColorGradient color,
                              MolangExpression brightness,
                              MolangExpression radius) implements ParticleModuleData {

    public static final MapCodec<LightModuleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ColorGradient.CODEC.fieldOf("gradient").forGetter(LightModuleData::color),
            MolangExpressionCodec.CODEC.fieldOf("brightness").forGetter(LightModuleData::brightness),
            MolangExpressionCodec.CODEC.fieldOf("radius").forGetter(LightModuleData::radius)
    ).apply(instance, LightModuleData::new));

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        if (this.color.isConstant() && this.brightness.isConstant() && this.radius.isConstant()) {
            StaticLightModule module = new StaticLightModule(this);
            if (module.isVisible()) {
                builder.addModule(module);
            }
        } else {
            builder.addModule(new DynamicLightModule(this));
        }
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.LIGHT;
    }
}
