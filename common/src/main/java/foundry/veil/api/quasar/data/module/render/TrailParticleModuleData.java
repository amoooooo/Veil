package foundry.veil.api.quasar.data.module.render;

import com.mojang.serialization.Codec;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.emitters.module.RenderParticleModule;
import foundry.veil.api.quasar.emitters.module.render.TrailParticleModule;
import foundry.veil.api.quasar.emitters.module.render.TrailSettings;
import foundry.veil.api.quasar.particle.ParticleModuleSet;
import foundry.veil.api.quasar.particle.RenderData;

import java.util.List;

/**
 * A module that adds trails to a particle.
 *
 * @see TrailSettings
 * @see RenderParticleModule
 * @see RenderData
 * WARNING: Trails add a lot of time to the rendering process, so use them sparingly.
 */
public record TrailParticleModuleData(List<TrailSettings> settings) implements ParticleModuleData {

    public static final Codec<TrailParticleModuleData> CODEC = TrailSettings.CODEC.listOf()
            .fieldOf("settings")
            .xmap(TrailParticleModuleData::new, TrailParticleModuleData::settings)
            .codec();

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule(new TrailParticleModule(this.settings));
    }

    @Override
    public ModuleType<?> getType() {
        return ParticleModuleTypeRegistry.TRAIL;
    }
}
