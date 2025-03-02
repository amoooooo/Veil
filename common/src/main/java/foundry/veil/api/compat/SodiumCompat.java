package foundry.veil.api.compat;

import foundry.veil.Veil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;

/**
 * Veil sodium compat implementation.
 *
 * @author Ocelot
 */
public interface SodiumCompat {

    /**
     * Retrieves the compat instance. This will be <code>null</code> if sodium is not installed.
     */
    @Nullable
    SodiumCompat INSTANCE = Veil.platform().isModLoaded("sodium") ? ServiceLoader.load(SodiumCompat.class).findFirst().orElse(null) : null;

    /**
     * @return Whether Sodium is loaded
     */
    static boolean isLoaded() {
        return INSTANCE != null;
    }

    /**
     * @return A map of all shader IDs loaded by sodium
     */
    Object2IntMap<ResourceLocation> getLoadedShaders();

    /**
     * Recompiles all shaders from source.
     */
    @ApiStatus.Experimental
    void recompile();

    /**
     * Sets the current shader active buffers. Will recompile/cache shaders as needed.
     *
     * @param activeBuffers The new active buffer flags
     */
    @ApiStatus.Internal
    void setActiveBuffers(int activeBuffers);

    /**
     * Marks all chunks in the view dirty.
     */
    void markChunksDirty();
}
