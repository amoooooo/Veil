package foundry.veil.forge.mixin;

import foundry.veil.api.resource.VeilDynamicRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {

    @Inject(method = "registryDirPath", at = @At("HEAD"), cancellable = true)
    private static void veilPath(ResourceLocation key, CallbackInfoReturnable<String> cir) {
        if (VeilDynamicRegistry.isLoading()) {
            cir.setReturnValue(key.getPath());
        }
    }
}
