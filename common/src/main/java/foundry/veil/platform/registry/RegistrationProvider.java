package foundry.veil.platform.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Utility class for multiloader registration.
 * <p>
 * Example usage:
 * <pre>{@code
 * public static final RegistrationProvider<Test> PROVIDER = RegistrationProvider.get(Test.REGISTRY, "modid");
 * public static final RegistryObject<Test> OBJECT = PROVIDER.register("object", () -> new Test());
 *
 * // The purpose of this method is to be called in the mod's constructor, in order to assure that the class is loaded, and that objects can be registered.
 * public static void loadClass(){}
 * }</pre>
 *
 * @param <T> the operand of the objects that this class registers
 */
@ApiStatus.Internal
public interface RegistrationProvider<T> {

    /**
     * The singleton instance of the {@link Factory}. This is different on each loader.
     */
    Factory FACTORY = ServiceLoader.load(Factory.class).findFirst().orElseThrow(() -> new RuntimeException("Failed to load registration provider"));

    /**
     * Gets a provider for specified {@code modId} and {@code resourceKey}.
     *
     * @param resourceKey the {@link ResourceKey} of the registry of the provider
     * @param modId       the mod id that the provider will register objects for
     * @param <T>         the operand of the provider
     * @return the provider
     */
    static <T> RegistrationProvider<T> get(ResourceKey<? extends Registry<T>> resourceKey, String modId) {
        return FACTORY.create(resourceKey, modId);
    }

    /**
     * Gets a provider for specified {@code modId} and {@code registry}.
     *
     * @param registry the {@link Registry} of the provider
     * @param modId    the mod id that the provider will register objects for
     * @param <T>      the operand of the provider
     * @return the provider
     */
    static <T> RegistrationProvider<T> get(Registry<T> registry, String modId) {
        return FACTORY.create(registry, modId);
    }

    /**
     * Registers an object.
     *
     * @param name     the name of the object
     * @param supplier a supplier of the object to register
     * @param <I>      the operand of the object
     * @return a wrapper containing the lazy registered object. <strong>Calling {@link RegistryObject#get() get} too early
     * on the wrapper might result in crashes!</strong>
     */
    default <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> supplier) {
        return this.register(new ResourceLocation(this.getModId(), name), supplier);
    }

    /**
     * Registers an object.
     *
     * @param id       the id of the object
     * @param supplier a supplier of the object to register
     * @param <I>      the operand of the object
     * @return a wrapper containing the lazy registered object. <strong>Calling {@link RegistryObject#get() get} too early
     * on the wrapper might result in crashes!</strong>
     */
    <I extends T> RegistryObject<I> register(ResourceLocation id, Supplier<? extends I> supplier);

    /**
     * @return An <strong>immutable</strong> view of all the objects currently registered
     */
    Collection<RegistryObject<T>> getEntries();

    /**
     * @return A wrapper of the underlying registry this provider registers into
     */
    Registry<T> asVanillaRegistry();

    /**
     * @return The mod id this provider registers objects for
     */
    String getModId();

    /**
     * Factory class for {@link RegistrationProvider registration providers}. <br>
     * This class is loaded using {@link java.util.ServiceLoader Service Loaders}, and only one
     * should exist per mod loader.
     */
    interface Factory {

        /**
         * Creates a {@link RegistrationProvider}.
         *
         * @param resourceKey the {@link ResourceKey} of the registry to create this provider for
         * @param modId       the mod id for which the provider will register objects
         * @param <T>         the operand of the provider
         * @return the provider
         */
        <T> RegistrationProvider<T> create(ResourceKey<? extends Registry<T>> resourceKey, String modId);

        /**
         * Creates a {@link RegistrationProvider}.
         *
         * @param registry the {@link Registry} to create this provider for
         * @param modId    the mod id for which the provider will register objects
         * @param <T>      the operand of the provider
         * @return the provider
         */
        default <T> RegistrationProvider<T> create(Registry<T> registry, String modId) {
            return this.create(registry.key(), modId);
        }
    }
}