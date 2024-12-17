package foundry.veil.impl.compat;

import foundry.veil.Veil;
import foundry.veil.ext.iris.IrisRenderTargetExtension;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

@ApiStatus.Internal
public interface IrisCompat {

    @Nullable
    IrisCompat INSTANCE = Veil.platform().isModLoaded("iris") ? ServiceLoader.load(IrisCompat.class).findFirst().orElse(null) : null;

    Set<ShaderInstance> getLoadedShaders();

    Map<String, IrisRenderTargetExtension> getRenderTargets();

    boolean areShadersLoaded();

    void recompile();
}
