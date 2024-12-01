package foundry.veil.forge.platform;

import foundry.veil.forge.ext.DeferredRegisterExtensions;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Supplier;

@ApiStatus.Internal
public class NeoForgeRegistrationFactory implements RegistrationProvider.Factory {

    @Override
    public <T> RegistrationProvider<T> create(ResourceKey<? extends Registry<T>> resourceKey, String modId) {
        ModContainer container = ModList.get().getModContainerById(modId).orElseThrow(() -> new NullPointerException("Cannot find mod container for id " + modId));
        if (!(container instanceof FMLModContainer forgeContainer)) {
            throw new ClassCastException("The container of the mod " + modId + " is not a FML one!");
        }

        DeferredRegister<T> register = DeferredRegister.create(resourceKey, modId);
        if (!BuiltInRegistries.REGISTRY.containsKey(resourceKey.location())) {
            register.makeRegistry(builder -> builder.sync(false));
        }
        register.register(Objects.requireNonNull(forgeContainer.getEventBus()));
        return new Provider<>(modId, register);
    }

    private static class Provider<T> implements RegistrationProvider<T> {

        private final String modId;
        private final DeferredRegister<T> registry;

        private final Set<RegistryObject<T>> entries = new HashSet<>();
        private final Set<RegistryObject<T>> entriesView = Collections.unmodifiableSet(this.entries);

        private Provider(String modId, DeferredRegister<T> registry) {
            this.modId = modId;
            this.registry = registry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> supplier) {
            DeferredHolder<I, I> obj = (DeferredHolder<I, I>) this.registry.register(name, supplier);
            RegistryObject<I> ro = new NeoForgeRegistryObject<>(obj);
            this.entries.add((RegistryObject<T>) ro);
            return ro;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <I extends T> RegistryObject<I> register(ResourceLocation id, Supplier<? extends I> supplier) {
            DeferredHolder<I, I> obj = (DeferredHolder<I, I>) ((DeferredRegisterExtensions<T>) this.registry).register(id, supplier);
            RegistryObject<I> ro = new NeoForgeRegistryObject<>(obj);
            this.entries.add((RegistryObject<T>) ro);
            return ro;
        }

        @Override
        public Collection<RegistryObject<T>> getEntries() {
            return this.entriesView;
        }

        @Override
        public Registry<T> asVanillaRegistry() {
            return Objects.requireNonNull(this.registry.getRegistry().get(), "Vanilla registry was not created");
        }

        @Override
        public String getModId() {
            return this.modId;
        }
    }

    private record NeoForgeRegistryObject<T, I extends T>(DeferredHolder<I, I> holder) implements RegistryObject<I> {

        @Override
        public ResourceKey<I> getResourceKey() {
            return this.holder.getKey();
        }

        @Override
        public boolean isPresent() {
            return this.holder.isBound();
        }

        @Override
        public I get() {
            return this.holder.get();
        }

        @Override
        public Holder<I> asHolder() {
            return this.holder;
        }
    }
}