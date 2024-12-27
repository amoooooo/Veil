package foundry.veil.api.compat;

import foundry.veil.Veil;
import foundry.veil.ext.iris.IrisRenderTargetExtension;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Veil iris compat implementation.
 *
 * @author Ocelot
 */
public interface IrisCompat {

    /**
     * Retrieves the compat instance. This will be <code>null</code> if iris is not installed.
     */
    @Nullable
    IrisCompat INSTANCE = Veil.platform().isModLoaded("iris") ? ServiceLoader.load(IrisCompat.class).findFirst().orElse(null) : null;

    /**
     * @return All loaded shaders created by Iris
     */
    Set<ShaderInstance> getLoadedShaders();

    /**
     * @return A mapping of all iris GBuffers by name
     */
    Map<String, IrisRenderTargetExtension> getRenderTargets();

    /**
     * @return Whether shaders are currently being used
     */
    boolean areShadersLoaded();

    /**
     * Recompiles iris shaders from source.
     */
    @ApiStatus.Experimental
    void recompile();
}
