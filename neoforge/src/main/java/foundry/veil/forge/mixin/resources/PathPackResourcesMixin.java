package foundry.veil.forge.mixin.resources;

import com.mojang.datafixers.util.Pair;
import cpw.mods.niofs.union.UnionFileSystem;
import foundry.veil.Veil;
import foundry.veil.ext.PackResourcesExtension;
import foundry.veil.forge.impl.ForgePackHooks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

@Mixin(PathPackResources.class)
public abstract class PathPackResourcesMixin implements PackResources, PackResourcesExtension {

    @Shadow
    @Final
    private Path root;

    @Shadow
    @Nullable
    public abstract IoSupplier<InputStream> getRootResource(String... elements);

    @Unique
    private String[] veil$icon;
    @Unique
    private boolean veil$iconBlur;

    @Override
    public void veil$listResources(PackResourceConsumer consumer) {
        String packId = this.packId();

        FileSystem fileSystem = this.root.getFileSystem();
        String separator = fileSystem.getSeparator();

        for (PackType type : PackType.values()) {
            Path assetPath = this.root.resolve(type.getDirectory());
            if (!Files.exists(assetPath)) {
                continue;
            }

            try {
                Files.walkFileTree(assetPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        String[] parts = assetPath.relativize(file).toString().replace(separator, "/").split("/", 2);
                        String namespace = parts.length == 1 ? "root" : parts[0];
                        String path = parts.length == 1 ? parts[0] : parts[1];
                        ResourceLocation name = ResourceLocation.tryBuild(namespace, path);

                        if (name != null) {
                            Path buildPath;
                            Path packPath = assetPath;
                            Path filePath = file;
                            Path modResourcePath = null;

                            // We have to do this hack so we can *actually* get access to the real files, not the forge wrapper
                            if (fileSystem instanceof UnionFileSystem unionFs && Files.isDirectory(unionFs.getPrimaryPath())) {
                                Path primaryPath = unionFs.getPrimaryPath();
                                Path buildDir = primaryPath.getParent().getParent().getParent();
                                buildPath = buildDir.resolve("resources").resolve(primaryPath.getFileName());

                                packPath = buildPath.getFileSystem().getPath(packPath.toString());
                                filePath = buildPath.resolve(file.toString());
                                modResourcePath = PackResourcesExtension.findDevPath(buildPath, filePath);
                            }

                            consumer.accept(type, name, packPath, filePath, modResourcePath);
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                Veil.LOGGER.warn("Failed to list resources in {} failed!", packId, e);
            }
        }
    }

    @Override
    public boolean veil$isStatic() {
        return this.root.getFileSystem() != FileSystems.getDefault();
    }

    @Override
    public List<Path> veil$getRawResourceRoots() {
        return Collections.singletonList(this.root);
    }

    @Override
    public @Nullable IoSupplier<InputStream> veil$getIcon() {
        if (this.veil$icon == null) {
            Pair<String, Boolean> pair = ForgePackHooks.getIcon(this.root);
            this.veil$icon = pair != null ? pair.getFirst().split("[/\\\\]") : new String[0];
            this.veil$iconBlur = pair != null && pair.getSecond();
        }
        return this.veil$icon.length == 0 ? this.getRootResource("pack.png") : this.getRootResource(this.veil$icon);
    }

    @Override
    public boolean veil$blurIcon() {
        return this.veil$iconBlur;
    }
}
