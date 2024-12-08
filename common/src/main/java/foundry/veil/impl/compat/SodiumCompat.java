package foundry.veil.impl.compat;

import foundry.veil.Veil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.ServiceLoader;

@ApiStatus.Internal
public interface SodiumCompat {

    SodiumCompat INSTANCE = Veil.platform().isModLoaded("sodium") ? ServiceLoader.load(SodiumCompat.class).findFirst().orElse(null) : null;

    Object2IntMap<ResourceLocation> getLoadedShaders();

    void recompile();

    void setActiveBuffers(int activeBuffers);

    void markChunksDirty();
}
