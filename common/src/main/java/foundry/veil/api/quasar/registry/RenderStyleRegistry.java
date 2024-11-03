package foundry.veil.api.quasar.registry;

import foundry.veil.Veil;
import foundry.veil.api.quasar.particle.RenderStyle;
import foundry.veil.platform.registry.RegistrationProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/**
 * Registry for all render styles.
 */
public class RenderStyleRegistry {

    public static final ResourceKey<Registry<RenderStyle>> REGISTRY_KEY = ResourceKey.createRegistryKey(Veil.veilPath("quasar/render_style"));
    private static final RegistrationProvider<RenderStyle> PROVIDER = RegistrationProvider.get(REGISTRY_KEY, Veil.MODID);
    public static final Registry<RenderStyle> REGISTRY = PROVIDER.asVanillaRegistry();

    public static final Supplier<RenderStyle.Cube> CUBE = register("cube", new RenderStyle.Cube());
    public static final Supplier<RenderStyle.Billboard> BILLBOARD = register("billboard", new RenderStyle.Billboard());

    @ApiStatus.Internal
    public static void bootstrap() {
    }

    private static <T extends RenderStyle> Supplier<T> register(String name, T shape) {
        return PROVIDER.register(name, () -> shape);
    }
}
