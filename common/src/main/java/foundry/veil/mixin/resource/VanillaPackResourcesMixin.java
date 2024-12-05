package foundry.veil.mixin.resource;

import foundry.veil.Veil;
import foundry.veil.ext.PackResourcesExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Mixin(VanillaPackResources.class)
public abstract class VanillaPackResourcesMixin implements PackResourcesExtension {

    @Shadow
    @Final
    private Map<PackType, List<Path>> pathsForType;

    @Shadow
    @Final
    private Set<String> namespaces;

    @Override
    public void veil$listResources(PackResourceConsumer consumer) {
        for (Map.Entry<PackType, List<Path>> entry : this.pathsForType.entrySet()) {
            PackType type = entry.getKey();

            for (Path basePath : entry.getValue()) {
                String separator = basePath.getFileSystem().getSeparator();

                for (String namespace : this.namespaces) {
                    Path nsPath = basePath.resolve(namespace);

                    try {
                        Files.walkFileTree(nsPath, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                                // Hack to make sure data pack isn't loaded as a resource pack
                                if (type == PackType.CLIENT_RESOURCES && dir.endsWith(PackType.SERVER_DATA.getDirectory())) {
                                    return FileVisitResult.SKIP_SUBTREE;
                                }
                                if (type == PackType.SERVER_DATA && dir.endsWith(PackType.CLIENT_RESOURCES.getDirectory())) {
                                    return FileVisitResult.SKIP_SUBTREE;
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                String filename = nsPath.relativize(file).toString().replace(separator, "/");
                                ResourceLocation name = ResourceLocation.tryBuild(namespace, filename);

                                if (name != null) {
                                    consumer.accept(type, name, nsPath, file, null);
                                }

                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                                if (file.endsWith("/data/realms")) { // This always fails, so just ignore it
                                    return FileVisitResult.CONTINUE;
                                }
                                return super.visitFileFailed(file, exc);
                            }
                        });
                    } catch (IOException e) {
                        Veil.LOGGER.warn("findResources in vanilla {} failed!", namespace, e);
                    }
                }
            }
        }
    }

    @Override
    public boolean veil$isStatic() {
        return true;
    }

    @Override
    public List<Path> veil$getRawResourceRoots() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable IoSupplier<InputStream> veil$getIcon() {
        return null;
    }
}
