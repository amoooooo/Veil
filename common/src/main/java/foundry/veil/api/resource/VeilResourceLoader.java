package foundry.veil.api.resource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public interface VeilResourceLoader {

    /**
     * Checks if the specified resource can load.
     *
     * @param packType        The type of pack the resource was loaded from
     * @param location        The path to load the resource from
     * @param filePath        The file path of the resource
     * @param modResourcePath The path to this resource in the build folder if in a dev environment
     * @return If this resource loader recognizes and can load the specified extension
     */
    boolean canLoad(PackType packType, ResourceLocation location, @Nullable Path filePath, @Nullable Path modResourcePath);

    /**
     * Loads the resource from the specified path.
     *
     * @param resourceManager The Veil resource manager instance
     * @param provider        The provider for vanilla resources
     * @param packType        The type of pack the resource was loaded from
     * @param location        The path to load the resource from
     * @param filePath        The file path of the resource
     * @param modResourcePath The path to this resource in the build folder if in a dev environment
     * @return The loaded resource
     */
    VeilResource<?> load(VeilResourceManager resourceManager, ResourceProvider provider, PackType packType, ResourceLocation location, @Nullable Path filePath, @Nullable Path modResourcePath) throws IOException;
}
