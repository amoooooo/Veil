package foundry.veil.impl.compat;

import foundry.veil.Veil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

@ApiStatus.Internal
public class SodiumShaderMap {

    private static final boolean SODIUM = Veil.platform().isModLoaded("sodium");
    private static Supplier<Object2IntMap<ResourceLocation>> loadedShadersSupplier = Object2IntMaps::emptyMap;

    public static Object2IntMap<ResourceLocation> getLoadedShaders() {
        return loadedShadersSupplier.get();
    }

    public static void setLoadedShadersSupplier(Supplier<Object2IntMap<ResourceLocation>> loadedShadersSupplier) {
        SodiumShaderMap.loadedShadersSupplier = loadedShadersSupplier;
    }

    public static boolean isEnabled() {
        return SODIUM;
    }
}
