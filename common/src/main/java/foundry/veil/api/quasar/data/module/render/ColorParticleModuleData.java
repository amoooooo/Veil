package foundry.veil.api.quasar.data.module.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.molang.MolangExpressionCodec;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.InitParticleModule;
import foundry.veil.api.quasar.emitters.module.render.ColorRenderModule;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import foundry.veil.impl.quasar.ColorGradient;
import gg.moonflower.molangcompiler.api.MolangExpression;

public record ColorParticleModuleData(ColorGradient gradient, MolangExpression interpolant) implements ParticleModuleData {

    public static final Codec<ColorParticleModuleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ColorGradient.CODEC.fieldOf("gradient").forGetter(ColorParticleModuleData::gradient),
            MolangExpressionCodec.CODEC.fieldOf("interpolant").forGetter(ColorParticleModuleData::interpolant)
    ).apply(instance, ColorParticleModuleData::new));


    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        if (this.gradient.isConstant() || this.interpolant.isConstant()) {
            builder.addModule((InitParticleModule) particle -> particle.getRenderData().setColor(this.gradient.getColor(particle.getEnvironment().safeResolve(this.interpolant))));
        } else {
            builder.addModule(new ColorRenderModule(this.gradient, this.interpolant));
        }
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.INIT_COLOR;
    }
}
