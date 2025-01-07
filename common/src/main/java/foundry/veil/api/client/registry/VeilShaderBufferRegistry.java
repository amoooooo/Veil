package foundry.veil.api.client.registry;

import foundry.veil.Veil;
import foundry.veil.api.client.render.CameraMatrices;
import foundry.veil.api.client.render.VeilShaderBufferLayout;
import foundry.veil.platform.registry.RegistrationProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/**
 * Registry for custom shader buffers created with {@link VeilShaderBufferLayout#builder(String)}.
 *
 * @author Ocelot
 */
public class VeilShaderBufferRegistry {

    public static final ResourceKey<Registry<VeilShaderBufferLayout<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(Veil.veilPath("shader_buffer"));
    private static final RegistrationProvider<VeilShaderBufferLayout<?>> PROVIDER = RegistrationProvider.get(REGISTRY_KEY, Veil.MODID);
    public static final Registry<VeilShaderBufferLayout<?>> REGISTRY = PROVIDER.asVanillaRegistry();

    public static final Supplier<VeilShaderBufferLayout<CameraMatrices>> VEIL_CAMERA = register("camera", CameraMatrices::createLayout);

    @ApiStatus.Internal
    public static void bootstrap() {
    }

    private static <T> Supplier<VeilShaderBufferLayout<T>> register(String name, Supplier<VeilShaderBufferLayout<T>> layout) {
        return PROVIDER.register(name, layout);
    }
}
