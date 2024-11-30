package foundry.veil.api.resource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @param location        The resource location path this resource is located at
 * @param filePath        The file path of this resource
 * @param modResourcePath The path to this resource in the build folder if in a dev environment
 * @param hidden          Whether this resource should appear in the resource panel
 */
public record VeilResourceInfo(PackType packType,
                               ResourceLocation location,
                               Path filePath,
                               @Nullable Path modResourcePath,
                               boolean hidden) {

    public Optional<Resource> getResource(VeilResourceManager resourceManager) {
        return resourceManager.resources(this).getResource(this.location);
    }

    public Resource getResourceOrThrow(VeilResourceManager resourceManager) throws FileNotFoundException {
        return this.getResource(resourceManager).orElseThrow(() -> new FileNotFoundException(this.location.toString()));
    }

    public InputStream open(VeilResourceManager resourceManager) throws IOException {
        return this.getResourceOrThrow(resourceManager).open();
    }

    public BufferedReader openAsReader(VeilResourceManager resourceManager) throws IOException {
        return this.getResourceOrThrow(resourceManager).openAsReader();
    }

    /**
     * @return The file name of this resource
     */
    public String fileName() {
        String path = this.location().getPath();
        String[] split = path.split("/");
        return split[split.length - 1];
    }

    /**
     * @return If this file cannot be accessed by the native file system
     */
    public boolean isStatic() {
        Path filePath = this.modResourcePath != null ? this.modResourcePath : this.filePath;
        return filePath == null || filePath.getFileSystem() != FileSystems.getDefault();
    }
}
