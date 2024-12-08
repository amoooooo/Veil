package foundry.veil.fabric.platform;

import foundry.veil.platform.VeilPlatform;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class FabricVeilPlatform implements VeilPlatform {

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.FABRIC;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean canAttachRenderdoc() {
        return true;
    }
}
