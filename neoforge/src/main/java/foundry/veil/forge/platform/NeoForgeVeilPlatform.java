package foundry.veil.forge.platform;

import foundry.veil.platform.VeilPlatform;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class NeoForgeVeilPlatform implements VeilPlatform {

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.NEOFORGE;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public boolean canAttachRenderdoc() {
        return !FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL);
    }

    @Override
    public boolean hasErrors() {
        return ModLoader.hasErrors();
    }
}
