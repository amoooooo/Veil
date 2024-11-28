package foundry.veil.impl.compat;

import foundry.veil.Veil;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.ApiStatus;

import java.util.ServiceLoader;
import java.util.Set;

@ApiStatus.Internal
public interface IrisCompat {

    IrisCompat INSTANCE = Veil.platform().isModLoaded("iris") ? ServiceLoader.load(IrisCompat.class).findFirst().orElse(null) : null;

    Set<ShaderInstance> getLoadedShaders();

    boolean areShadersLoaded();

    void recompile();
}
