package foundry.veil.api.quasar.data;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.particle.ParticleEmitter;
import foundry.veil.api.resource.VeilDynamicRegistry;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public final class QuasarParticles {

    public static final ResourceKey<Registry<ParticleModuleData>> INIT_MODULES = createRegistryKey("quasar/modules/init");
    public static final ResourceKey<Registry<ParticleModuleData>> UPDATE_MODULES = createRegistryKey("quasar/modules/update");
    public static final ResourceKey<Registry<ParticleModuleData>> RENDER_MODULES = createRegistryKey("quasar/modules/render");
    public static final ResourceKey<Registry<QuasarParticleData>> PARTICLE_DATA = createRegistryKey("quasar/modules/particle_data");
    public static final ResourceKey<Registry<ParticleSettings>> PARTICLE_SETTINGS = createRegistryKey("quasar/modules/emitter/particle");
    public static final ResourceKey<Registry<EmitterShapeSettings>> EMITTER_SHAPE_SETTINGS = createRegistryKey("quasar/modules/emitter/shape");
    public static final ResourceKey<Registry<ParticleEmitterData>> EMITTER = createRegistryKey("quasar/emitters");

    private static final SuggestionProvider<?> EMITTER_SUGGESTION_PROVIDER = (unused, builder) -> registryAccess().registry(EMITTER).map(registry -> SharedSuggestionProvider.suggestResource(registry.keySet(), builder)).orElseGet(Suggestions::empty);
    private static final List<RegistryDataLoader.RegistryData<?>> REGISTRIES = List.of(
            new RegistryDataLoader.RegistryData<>(INIT_MODULES, ParticleModuleData.INIT_DIRECT_CODEC, false),
            new RegistryDataLoader.RegistryData<>(UPDATE_MODULES, ParticleModuleData.UPDATE_DIRECT_CODEC, false),
            new RegistryDataLoader.RegistryData<>(RENDER_MODULES, ParticleModuleData.RENDER_DIRECT_CODEC, false),
            new RegistryDataLoader.RegistryData<>(PARTICLE_DATA, QuasarParticleData.DIRECT_CODEC, false),
            new RegistryDataLoader.RegistryData<>(PARTICLE_SETTINGS, ParticleSettings.DIRECT_CODEC, false),
            new RegistryDataLoader.RegistryData<>(EMITTER_SHAPE_SETTINGS, EmitterShapeSettings.DIRECT_CODEC, false),
            new RegistryDataLoader.RegistryData<>(EMITTER, ParticleEmitterData.DIRECT_CODEC, false)
    );
    private static RegistryAccess registryAccess = RegistryAccess.EMPTY;

    private QuasarParticles() {
    }

    private static <T> ResourceKey<Registry<T>> createRegistryKey(String name) {
        return ResourceKey.createRegistryKey(Veil.veilPath(name));
    }

    @SuppressWarnings("unchecked")
    public static <T extends SharedSuggestionProvider> SuggestionProvider<T> emitterSuggestionProvider() {
        return (SuggestionProvider<T>) EMITTER_SUGGESTION_PROVIDER;
    }

    public static RegistryAccess registryAccess() {
        return registryAccess;
    }

    @ApiStatus.Internal
    public static class Reloader extends SimplePreparableReloadListener<VeilDynamicRegistry.Data> {

        @Override
        protected VeilDynamicRegistry.Data prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            return VeilDynamicRegistry.loadRegistries(resourceManager, REGISTRIES);
        }

        @Override
        protected void apply(VeilDynamicRegistry.Data preparations, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            registryAccess = preparations.registryAccess();
            ParticleEmitter.clearErrors();

            String msg = VeilDynamicRegistry.printErrors(preparations.errors());
            if (msg != null) {
                Veil.LOGGER.error("Quasar registry loading errors:{}", msg);
            }

            Veil.LOGGER.info("Loaded {} quasar particles", registryAccess.registryOrThrow(EMITTER).size());
            VeilRenderSystem.renderer().getParticleManager().clear();
        }

        @Override
        public String getName() {
            return QuasarParticles.class.getSimpleName();
        }
    }
}
