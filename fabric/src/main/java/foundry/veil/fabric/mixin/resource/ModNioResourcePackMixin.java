package foundry.veil.fabric.mixin.resource;

import foundry.veil.Veil;
import foundry.veil.ext.PackResourcesExtension;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Mixin(ModNioResourcePack.class)
public abstract class ModNioResourcePackMixin implements ModResourcePack, PackResourcesExtension {

    @Shadow
    @Final
    private List<Path> basePaths;

    @Shadow
    @Final
    private Map<PackType, Set<String>> namespaces;

    @Shadow
    @Final
    private ModContainer mod;

    @Override
    public void veil$listResources(PackResourceConsumer consumer) {
        for (Path basePath : this.basePaths) {
            String separator = basePath.getFileSystem().getSeparator();

            for (Map.Entry<PackType, Set<String>> entry : this.namespaces.entrySet()) {
                PackType type = entry.getKey();

                for (String namespace : entry.getValue()) {
                    Path nsPath = basePath.resolve(type.getDirectory()).resolve(namespace);
                    if (!Files.exists(nsPath)) {
                        continue;
                    }

                    try {
                        Files.walkFileTree(nsPath, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                String filename = nsPath.relativize(file).toString().replace(separator, "/");
                                ResourceLocation name = ResourceLocation.tryBuild(namespace, filename);

                                if (name != null) {
                                    consumer.accept(type, name, nsPath, file, PackResourcesExtension.findDevPath(basePath, file));
                                }

                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        Veil.LOGGER.warn("findResources in namespace {}, mod {} failed!", namespace, this.mod.getMetadata().getId(), e);
                    }
                }
            }
        }
    }

    @Override
    public boolean veil$isStatic() {
        boolean dynamic = false;

        for (Path basePath : this.basePaths) {
            if (basePath.getFileSystem() == FileSystems.getDefault()) {
                dynamic = true;
            }
        }

        return !dynamic;
    }

    @Override
    public List<Path> veil$getRawResourceRoots() {
        return this.basePaths.stream().flatMap(path -> PackResourcesExtension.findDevPaths(path, path).stream()).toList();
    }

    @Override
    public @Nullable IoSupplier<InputStream> veil$getIcon() {
        ModMetadata metadata = this.mod.getMetadata();
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(metadata.getId()).orElseThrow();
        return metadata.getIconPath(20).flatMap(modContainer::findPath).<IoSupplier<InputStream>>map(path -> () -> Files.newInputStream(path)).orElse(null);
    }

    @Override
    public boolean veil$blurIcon() {
        return false;
    }

    @Override
    public Stream<PackResources> veil$listPacks() {
        ModMetadata metadata = this.mod.getMetadata();
        String id = metadata.getId();
        if (!"fabric-api".equalsIgnoreCase(id) && id.startsWith("fabric") && metadata.containsCustomValue("fabric-api:module-lifecycle")) {
            // Skip fabric apis
            return Stream.empty();
        }

        return Stream.of(this);
    }
}
