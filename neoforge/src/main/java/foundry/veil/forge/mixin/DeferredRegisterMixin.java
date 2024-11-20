package foundry.veil.forge.mixin;

import foundry.veil.forge.ext.DeferredRegisterExtensions;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(DeferredRegister.class)
public abstract class DeferredRegisterMixin<T> implements DeferredRegisterExtensions<T> {

    @Shadow
    private boolean seenRegisterEvent;

    @Shadow
    protected abstract <I extends T> DeferredHolder<T, I> createHolder(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation key);

    @Shadow
    @Final
    private ResourceKey<? extends Registry<T>> registryKey;

    @Shadow
    @Final
    private Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries;

    @Override
    public <I extends T> DeferredHolder<T, I> register(ResourceLocation key, Function<ResourceLocation, ? extends I> func) {
        if (seenRegisterEvent) {
            throw new IllegalStateException("Cannot register new entries to DeferredRegister after RegisterEvent has been fired.");
        }

        Objects.requireNonNull(key);
        Objects.requireNonNull(func);

        DeferredHolder<T, I> holder = this.createHolder(this.registryKey, key);
        if (this.entries.putIfAbsent(holder, () -> func.apply(key)) != null) {
            throw new IllegalArgumentException("Duplicate registration " + key);
        }

        return holder;
    }
}
