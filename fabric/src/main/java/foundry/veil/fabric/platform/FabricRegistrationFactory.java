package foundry.veil.fabric.platform;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

@ApiStatus.Internal
public class FabricRegistrationFactory implements RegistrationProvider.Factory {

    @SuppressWarnings("unchecked")
    @Override
    public <T> RegistrationProvider<T> create(ResourceKey<? extends Registry<T>> key, String modId) {
        Registry<?> reg = BuiltInRegistries.REGISTRY.get(key.location());
        if (reg == null) {
            reg = FabricRegistryBuilder.createSimple((ResourceKey<Registry<T>>) key).buildAndRegister();
        }
        return new Provider<>(modId, (Registry<T>) reg);
    }

    @Override
    public <T> RegistrationProvider<T> create(Registry<T> registry, String modId) {
        return new Provider<>(modId, registry);
    }

    private static class Provider<T> implements RegistrationProvider<T> {

        private final String modId;
        private final Registry<T> registry;

        private final Set<RegistryObject<T>> entries = new HashSet<>();
        private final Set<RegistryObject<T>> entriesView = Collections.unmodifiableSet(this.entries);

        private Provider(String modId, Registry<T> registry) {
            this.modId = modId;
            this.registry = registry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends T> RegistryObject<I> register(ResourceLocation id, Supplier<? extends I> supplier) {
            I value = Registry.register(this.registry, id, supplier.get());
            ResourceKey<I> key = ResourceKey.create((ResourceKey<? extends Registry<I>>) this.registry.key(), id);
            RegistryObject<I> object = new FabricRegistryObject<>(this.registry, key, value);
            this.entries.add((RegistryObject<T>) object);
            return object;
        }

        @Override
        public Collection<RegistryObject<T>> getEntries() {
            return this.entriesView;
        }

        @Override
        public Registry<T> asVanillaRegistry() {
            return this.registry;
        }

        @Override
        public String getModId() {
            return this.modId;
        }
    }

    private record FabricRegistryObject<T, I>(Registry<T> registry,
                                              ResourceKey<I> key,
                                              I value) implements RegistryObject<I> {

        @Override
        public ResourceKey<I> getResourceKey() {
            return this.key;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public I get() {
            return this.value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Holder<I> asHolder() {
            return (Holder<I>) this.registry.getHolderOrThrow((ResourceKey<T>) this.key);
        }
    }
}