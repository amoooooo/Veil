package foundry.veil.forge.impl;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.concurrent.ConcurrentConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mojang.datafixers.util.Pair;
import cpw.mods.niofs.union.UnionFileSystem;
import foundry.veil.Veil;
import net.neoforged.fml.loading.moddiscovery.NightConfigWrapper;
import net.neoforged.neoforgespi.language.IConfigurable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApiStatus.Internal
public final class ForgePackHooks {

    private ForgePackHooks() {
    }

    public static @Nullable Pair<String, Boolean> getIcon(Path path) {
        if (!(path.getFileSystem() instanceof UnionFileSystem)) {
            return null;
        }

        Path file = path.resolve("META-INF").resolve("neoforge.mods.toml");
        if (!Files.isRegularFile(file)) {
            return null;
        }

        try {
            FileConfig fileConfig = FileConfig.builder(file).build();
            fileConfig.load();
            fileConfig.close();

            NightConfigWrapper config = new NightConfigWrapper(copyConfig(fileConfig));

            List<? extends IConfigurable> mods = config.getConfigList("mods");
            if (mods.isEmpty()) {
                return null;
            }

            IConfigurable mod = mods.getFirst();
            String logoFile = mod.<String>getConfigElement("logoFile").orElse(null);
            boolean logoBlur = mod.<Boolean>getConfigElement("logoBlur").orElse(true);
            return logoFile != null ? Pair.of(logoFile, logoBlur) : null;
        } catch (Throwable t) {
            Veil.LOGGER.error("Failed to load mod icon", t);
            return null;
        }
    }

    private static UnmodifiableConfig copyConfig(ConcurrentConfig config) {
        TomlFormat format = TomlFormat.instance();
        return format.createParser().parse(format.createWriter().writeToString(config)).unmodifiable();
    }
}
