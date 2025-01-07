package foundry.veil.mixin.registry.accessor;

import com.mojang.serialization.Lifecycle;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(RegistryDataLoader.RegistryData.class)
public interface RegistryDataAccessor {

    @Invoker
    RegistryDataLoader.Loader<?> invokeCreate(Lifecycle lifecycle, Map<ResourceKey<?>, Exception> errors);
}
