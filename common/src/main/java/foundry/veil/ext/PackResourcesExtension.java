package foundry.veil.ext;

import foundry.veil.Veil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public interface PackResourcesExtension {

    String BUILD_RESOURCES_NAME = "resources";

    void veil$listResources(PackResourceConsumer consumer);

    @Nullable
    IoSupplier<InputStream> veil$getIcon();

    boolean veil$isStatic();

    List<Path> veil$getRawResourceRoots();

    default Stream<PackResources> veil$listPacks() {
        return Stream.of((PackResources) this);
    }

    static @Nullable Path findDevPath(Path root, Path file) {
        // We're in a Zip file
        if (file.getFileSystem() != FileSystems.getDefault()) {
            return null;
        }

        // Attempt to find actual resources, not gradle copy
        if (file.getNameCount() > 3) {
            try {
                Path buildRoot = root;
                String sourceRoot = null;
                Path localPath = null;
                while (buildRoot != null && buildRoot.getFileName() != null && !"build".equals(buildRoot.getFileName().toString())) {
                    Path parent = buildRoot.getParent();
                    if (parent != null && parent.getFileName() != null && parent.getFileName().toString().equals(BUILD_RESOURCES_NAME)) {
                        sourceRoot = buildRoot.getFileName().toString();
                        localPath = buildRoot.relativize(file);
                    }
                    buildRoot = parent;
                }

                // We aren't in a build output, so don't try
                if (buildRoot == null || sourceRoot == null) {
                    return null;
                }

                Path defaultRoot = buildRoot.getParent();
                if (defaultRoot == null) {
                    return null;
                }

                Path buildPath = defaultRoot.resolve("src").resolve(sourceRoot).resolve("resources").resolve(localPath);
                if (Files.exists(buildPath)) {
                    return buildPath;
                }

                Path projectRoot = defaultRoot.getParent();
                if (projectRoot == null) {
                    return null;
                }

                // This sucks, but we have to scan all roots :/
                try (Stream<Path> walk = Files.list(projectRoot).filter(path -> Files.isDirectory(path) && !path.startsWith(defaultRoot))) {
                    for (Path possibleRoot : walk.toList()) {
                        Path resourcesPath = possibleRoot.resolve("src").resolve(sourceRoot).resolve("resources").resolve(localPath);
                        if (Files.exists(resourcesPath)) {
                            return resourcesPath;
                        }
                    }
                }
            } catch (Exception e) {
                Veil.LOGGER.error("Failed to find IDE source root", e);
            }
        }
        return null;
    }

    static List<Path> findDevPaths(Path root, Path file) {
        List<Path> paths = new ArrayList<>();

        // We're in a Zip file
        if (file.getFileSystem() != FileSystems.getDefault()) {
            return paths;
        }

        // Attempt to find actual resources, not gradle copy
        if (file.getNameCount() > 3) {
            try {
                Path buildRoot = root;
                String sourceRoot = null;
                Path localPath = null;
                while (buildRoot != null && buildRoot.getFileName() != null && !"build".equals(buildRoot.getFileName().toString())) {
                    Path parent = buildRoot.getParent();
                    if (parent != null && parent.getFileName() != null && parent.getFileName().toString().equals(BUILD_RESOURCES_NAME)) {
                        sourceRoot = buildRoot.getFileName().toString();
                        localPath = buildRoot.relativize(file);
                    }
                    buildRoot = parent;
                }

                // We aren't in a build output, so don't try
                if (buildRoot == null || sourceRoot == null) {
                    return paths;
                }

                Path defaultRoot = buildRoot.getParent();
                if (defaultRoot == null) {
                    return paths;
                }

                Path buildPath = defaultRoot.resolve("src").resolve(sourceRoot).resolve("resources").resolve(localPath);
                if (Files.exists(buildPath)) {
                    paths.add(buildPath);
                }

                Path projectRoot = defaultRoot.getParent();
                if (projectRoot == null) {
                    return paths;
                }

                // This sucks, but we have to scan all roots :/
                try (Stream<Path> walk = Files.list(projectRoot).filter(path -> Files.isDirectory(path) && !path.startsWith(defaultRoot))) {
                    for (Path possibleRoot : walk.toList()) {
                        Path resourcesPath = possibleRoot.resolve("src").resolve(sourceRoot).resolve("resources").resolve(localPath);
                        if (Files.exists(resourcesPath)) {
                            paths.add(resourcesPath);
                        }
                    }
                }
            } catch (Exception e) {
                Veil.LOGGER.error("Failed to find IDE source root", e);
            }
        }
        return paths;
    }

    @FunctionalInterface
    interface PackResourceConsumer {

        void accept(@Nullable PackType packType, ResourceLocation name, Path packPath, Path filePath, @Nullable Path modResourcePath);
    }
}
