package foundry.veil.fabric.mixin.resource;

import foundry.veil.Veil;
import foundry.veil.ext.PackResourcesExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

@Mixin(PathPackResources.class)
public abstract class PathPackResourcesMixin implements PackResources, PackResourcesExtension {

    @Shadow
    @Nullable
    public abstract IoSupplier<InputStream> getRootResource(String... elements);

    @Shadow
    @Final
    private Path root;

    @Override
    public void veil$listResources(PackResourceConsumer consumer) {
        String packId = this.packId();

        String separator = this.root.getFileSystem().getSeparator();

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
                            consumer.accept(type, name, assetPath, file, PackResourcesExtension.findDevPath(root, file));
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
        return Collections.singletonList(this.root.relativize(Minecraft.getInstance().gameDirectory.toPath()));
    }

    @Override
    public @Nullable IoSupplier<InputStream> veil$getIcon() {
        return this.getRootResource("pack.png");
    }

    @Override
    public boolean veil$blurIcon() {
        return false;
    }
}
