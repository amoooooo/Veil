package foundry.veil.api.quasar.registry;

import foundry.veil.Veil;
import foundry.veil.api.quasar.emitters.shape.*;
import foundry.veil.platform.registry.RegistrationProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/**
 * Registry for all emitter shapes.
 */
public class EmitterShapeRegistry {

    public static final ResourceKey<Registry<EmitterShape>> REGISTRY_KEY = ResourceKey.createRegistryKey(Veil.veilPath("quasar/emitter_shape"));
    private static final RegistrationProvider<EmitterShape> PROVIDER = RegistrationProvider.get(REGISTRY_KEY, Veil.MODID);
    public static final Registry<EmitterShape> REGISTRY = PROVIDER.asVanillaRegistry();

    public static final Supplier<Point> POINT = register("point", new Point());
    public static final Supplier<Hemisphere> HEMISPHERE = register("hemisphere", new Hemisphere());
    public static final Supplier<Cylinder> CYLINDER = register("cylinder", new Cylinder());
    public static final Supplier<Sphere> SPHERE = register("sphere", new Sphere());
    public static final Supplier<Cube> CUBE = register("cube", new Cube());
    public static final Supplier<Torus> TORUS = register("torus", new Torus());
    public static final Supplier<Disc> DISC = register("disc", new Disc());
    public static final Supplier<Plane> PLANE = register("plane", new Plane());

    @ApiStatus.Internal
    public static void bootstrap() {
    }

    private static <T extends EmitterShape> Supplier<T> register(String name, T shape) {
        return PROVIDER.register(name, () -> shape);
    }
}
