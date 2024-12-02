package foundry.veil.impl.resource.loader;

import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceLoader;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.api.resource.type.LanguageResource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public class LanguageResourceLoader implements VeilResourceLoader {

    public static final String EXTENSION = ".json";
    public static final String PATH = "lang";

    @Override
    public boolean canLoad(PackType packType, ResourceLocation location, @Nullable Path filePath, @Nullable Path modResourcePath) {
        if (packType != PackType.CLIENT_RESOURCES) {
            return false;
        }

        final String path = location.getPath();
        if (path.startsWith(PATH)) {
            return path.endsWith(EXTENSION);
        }
        return false;
    }

    @Override
    public VeilResource<?> load(VeilResourceManager resourceManager, ResourceProvider provider, PackType packType, ResourceLocation location, @Nullable Path filePath, @Nullable Path modResourcePath) throws IOException {
        VeilResourceInfo info = new VeilResourceInfo(packType, location, filePath, modResourcePath, false);
        return new LanguageResource(info);
    }
}
