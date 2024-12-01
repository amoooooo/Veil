package foundry.veil.platform.registry;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Represents a lazy wrapper for registry object.
 *
 * @param <T> the operand of the object
 */
public interface RegistryObject<T> extends Supplier<T> {

    /**
     * Gets the {@link ResourceKey} of the registry of the object wrapped.
     *
     * @return the {@link ResourceKey} of the registry
     */
    ResourceKey<T> getResourceKey();

    /**
     * Gets the id of the object.
     *
     * @return the id of the object
     */
    default ResourceLocation getId() {
        return this.getResourceKey().location();
    }

    /**
     * @return If the initializer has been registered and assigned
     */
    boolean isPresent();

    /**
     * Gets the object behind this wrapper.
     *
     * @return the object behind this wrapper
     * @throws NullPointerException If {@link #isPresent()} returns <code>false</code>
     */
    @Override
    T get();

    /**
     * Gets this object wrapped in a vanilla {@link Holder}.
     *
     * @return the holder
     */
    Holder<T> asHolder();
}