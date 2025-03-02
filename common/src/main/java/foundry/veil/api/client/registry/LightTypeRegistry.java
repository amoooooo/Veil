package foundry.veil.api.client.registry;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.*;
import foundry.veil.api.client.render.light.renderer.IndirectLightRenderer;
import foundry.veil.api.client.render.light.renderer.LightTypeRenderer;
import foundry.veil.impl.client.editor.LightInspector;
import foundry.veil.impl.client.render.light.AreaLightRenderer;
import foundry.veil.impl.client.render.light.DirectionalLightRenderer;
import foundry.veil.impl.client.render.light.IndirectPointLightRenderer;
import foundry.veil.impl.client.render.light.InstancedPointLightRenderer;
import foundry.veil.platform.registry.RegistrationProvider;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Registry for all light types.
 */
public final class LightTypeRegistry {

    public static final ResourceKey<Registry<LightType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(Veil.veilPath("light_type"));
    private static final RegistrationProvider<LightType<?>> PROVIDER = RegistrationProvider.get(REGISTRY_KEY, Veil.MODID);
    public static final Registry<LightType<?>> REGISTRY = PROVIDER.asVanillaRegistry();

    public static final Supplier<LightType<DirectionalLight>> DIRECTIONAL = register("directional", DirectionalLightRenderer::new, (level, camera) -> new DirectionalLight().setTo(camera).setDirection(0, -1, 0));
    public static final Supplier<LightType<PointLight>> POINT = register("point", () -> {
        boolean supported = VeilRenderSystem.multiDrawIndirectSupported();
        if (supported) {
            Veil.LOGGER.info("Using Indirect Point Light Renderer");
            return new IndirectPointLightRenderer();
        } else {
            Veil.LOGGER.info("Using Instanced Point Light Renderer");
            return new InstancedPointLightRenderer();
        }
    }, (level, camera) -> new PointLight().setTo(camera).setRadius(15.0F));
    public static final Supplier<LightType<AreaLight>> AREA = register("area", AreaLightRenderer::new, (level, camera) -> new AreaLight().setDistance(15.0F).setTo(camera));

    private LightTypeRegistry() {
    }

    @ApiStatus.Internal
    public static void bootstrap() {
    }

    private static <T extends Light> Supplier<LightType<T>> register(String name, RendererFactory<T> factory, @Nullable DebugLightFactory debugFactory) {
        return PROVIDER.register(name, () -> new LightType<>(factory, debugFactory));
    }

    public record LightType<T extends Light>(RendererFactory<T> rendererFactory,
                                             @Nullable DebugLightFactory debugLightFactory) {
    }

    /**
     * Creates the renderer for lights when requested.
     *
     * @param <T> The type of light the renderer needs to draw
     */
    @FunctionalInterface
    public interface RendererFactory<T extends Light> {

        /**
         * @return A new renderer for lights
         */
        LightTypeRenderer<T> createRenderer();
    }

    /**
     * Creates debug lights for the {@link LightInspector}.
     */
    @FunctionalInterface
    public interface DebugLightFactory {

        /**
         * Creates a new light in the level. The position is set automatically for {@link PositionedLight}.
         *
         * @param level  The level the light is in
         * @param camera The camera the light is being spawned at
         * @return The new light created
         */
        Light createDebugLight(ClientLevel level, Camera camera);
    }
}
