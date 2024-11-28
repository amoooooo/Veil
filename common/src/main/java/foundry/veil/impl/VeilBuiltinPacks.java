package foundry.veil.impl;

import foundry.veil.Veil;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class VeilBuiltinPacks {

    public static void registerPacks(VeilBuiltinPacks.Context context) {
        if (Veil.platform().isDevelopmentEnvironment()) {
            context.register(Veil.veilPath("test_shaders"), false);
            context.register(Veil.veilPath("test_particles"), false);
            context.register(Veil.veilPath("volumetric_fog"), false);
        }

//        if (!Veil.SODIUM) {
//            context.register(VeilDeferredRenderer.PACK_ID, false);
//        }
    }

    @FunctionalInterface
    public interface Context {

        void register(ResourceLocation id, boolean defaultEnabled);
    }
}
