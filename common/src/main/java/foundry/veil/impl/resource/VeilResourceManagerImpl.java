package foundry.veil.impl.resource;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceLoader;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.api.resource.type.UnknownResource;
import foundry.veil.api.util.CompositeReloadListener;
import foundry.veil.ext.PackResourcesExtension;
import foundry.veil.impl.resource.loader.*;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Manages all veil resources
 */
@ApiStatus.Internal
public class VeilResourceManagerImpl implements VeilResourceManager, NativeResource {

    private static final AtomicInteger WATCHER_ID = new AtomicInteger(1);
    private final ObjectList<VeilResourceLoader> loaders;
    private final List<VeilPackResources> packResources;
    private final Object2ObjectMap<Path, PackResourceListener> watchers;

    private ResourceManager serverResourceManager = ResourceManager.Empty.INSTANCE;

    public VeilResourceManagerImpl() {
        this.loaders = new ObjectArrayList<>(8);
        this.packResources = new LinkedList<>();
        this.watchers = new Object2ObjectArrayMap<>();
    }

    public void addVeilLoaders(VeilRenderer renderer) {
        this.addLoader(new ShaderResourceLoader(renderer.getShaderManager()));
        this.addLoader(new ShaderIncludeLoader());
        this.addLoader(new PostPipelineResourceLoader());
        this.addLoader(new FramebufferResourceLoader());
        this.addLoader(new RenderTypeResourceLoader());
        this.addLoader(new TextureResourceLoader());
        this.addLoader(new McMetaResourceLoader());
        this.addLoader(new TextResourceLoader());
    }

    /**
     * Adds a resource loader to the resource manager
     */
    public void addLoader(VeilResourceLoader loader) {
        this.loaders.add(loader);
    }

    private void loadPack(ResourceManager resourceManager, VeilPackResources resources, Object2ObjectMap<Path, PackResourceListener> watchers, PackType type, PackResources packResources) {
        if (packResources instanceof PackResourcesExtension ext) {
            try {
                ext.veil$listResources((packType, loc, packPath, path, modResourcePath) -> {
                    if (packType != type) {
                        return;
                    }

                    try {
                        VeilResource<?> resource = this.visitResource(packType, resourceManager, loc, path, modResourcePath);
                        resources.add(packType, loc, resource);

                        if (!resource.resourceInfo().isStatic()) {
                            Path listenPath = modResourcePath != null ? modResourcePath : path;
                            if (listenPath != null) {
                                try {
                                    watchers.computeIfAbsent(packPath, PackResourceListener::new).listen(resource, listenPath);
                                } catch (Exception e) {
                                    Veil.LOGGER.error("Failed to listen to resource: {}", resource.resourceInfo().location(), e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Veil.LOGGER.error("Error loading resource: {}", loc, e);
                    }
                });
                return;
            } catch (Exception e) {
                Veil.LOGGER.error("Failed to load resources from {}", this.getClass().getSimpleName(), e);
            }
        }

        for (String namespace : packResources.getNamespaces(type)) {
            packResources.listResources(type, namespace, "", (loc, inputStreamIoSupplier) -> {
                try {
                    resources.add(type, loc, this.visitResource(type, resourceManager, loc, null, null));
                } catch (Exception e) {
                    Veil.LOGGER.error("Error loading resource: {}", loc, e);
                }
            });
        }
    }

    private VeilResource<?> visitResource(@Nullable PackType packType, ResourceProvider provider, ResourceLocation loc, @Nullable Path path, @Nullable Path modResourcePath) throws IOException {
        for (VeilResourceLoader loader : this.loaders) {
            if (loader.canLoad(packType, loc, path, modResourcePath)) {
                return loader.load(this, provider, packType, loc, path, modResourcePath);
            }
        }

        // If no loaders can load the resource, add it as an unknown resource
        return new UnknownResource(new VeilResourceInfo(packType, loc, path, modResourcePath, false));
    }

    public PreparableReloadListener createReloadListener() {
        return CompositeReloadListener.of(new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return VeilResourceManagerImpl.this.reloadClient(preparationBarrier, resourceManager, prepareProfiler, applyProfiler, backgroundExecutor, gameExecutor);
            }

            @Override
            public String getName() {
                return VeilResourceManager.class.getSimpleName() + " Client";
            }
        }, new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return VeilResourceManagerImpl.this.reloadServer(preparationBarrier, resourceManager, prepareProfiler, applyProfiler, backgroundExecutor, gameExecutor);
            }

            @Override
            public String getName() {
                return VeilResourceManager.class.getSimpleName() + " Server";
            }
        });
    }

    private CompletableFuture<Void> reloadClient(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            List<VeilPackResources> packs = new LinkedList<>();
            Object2ObjectMap<Path, PackResourceListener> watchers = new Object2ObjectArrayMap<>();
            resourceManager.listPacks().flatMap(pack -> pack instanceof PackResourcesExtension extension ? extension.veil$listPacks() : Stream.of(pack)).forEach(pack -> {
                VeilPackResources resources = new VeilPackResources(pack.packId());
                this.loadPack(resourceManager, resources, watchers, PackType.CLIENT_RESOURCES, pack);
                packs.add(resources);

                if (pack instanceof PackResourcesExtension extension) {
                    IoSupplier<InputStream> icon = extension.veil$getIcon();
                    if (icon != null) {
                        try {
                            NativeImage image = NativeImage.read(icon.get());
                            RenderSystem.recordRenderCall(() -> {
                                try (image) {
                                    resources.loadIcon(image);
                                }
                            });
                        } catch (Exception e) {
                            Veil.LOGGER.error("Failed to load icon for pack: {}", pack.packId(), e);
                        }
                    }
                }
            });
            return new Preparations(packs, watchers);
        }, backgroundExecutor).thenCompose(preparationBarrier::wait).thenAcceptAsync(preparations -> {
            this.free();
            this.packResources.addAll(preparations.packs());
            this.watchers.putAll(preparations.watchers());
        }, gameExecutor);
    }

    private CompletableFuture<Void> reloadServer(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> this.serverResourceManager = new MultiPackResourceManager(PackType.SERVER_DATA, resourceManager.listPacks().toList()), backgroundExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(serverResourceManager -> this.serverResourceManager = serverResourceManager, gameExecutor);
    }

    @Override
    public ResourceManager clientResources() {
        return Minecraft.getInstance().getResourceManager();
    }

    @Override
    public ResourceManager serverResources() {
        return this.serverResourceManager;
    }

    @Override
    public @Nullable VeilResource<?> getVeilResource(String namespace, String path) {
        for (VeilPackResources pack : this.packResources) {
            VeilResource<?> resource = pack.getVeilResource(namespace, path);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public void free() {
        for (PackResourceListener listener : this.watchers.values()) {
            try {
                listener.close();
            } catch (IOException e) {
                Veil.LOGGER.error("Error closing watch service: {}", listener.getPath(), e);
            }
        }
        this.watchers.clear();
        for (VeilPackResources resources : this.packResources) {
            resources.free();
        }
        this.packResources.clear();
        WATCHER_ID.set(1);
    }

    /**
     * @return All pack folders
     */
    public List<VeilPackResources> getAllPacks() {
        return this.packResources;
    }

    private record Preparations(List<VeilPackResources> packs, Object2ObjectMap<Path, PackResourceListener> watchers) {
    }

    private static class PackResourceListener implements Closeable {

        private final Path path;
        private final WatchService watchService;
        private final ObjectSet<Path> watchedDirectories;
        private final ObjectSet<Path> ignoredPaths;
        private final Object2ObjectMap<Path, VeilResource<?>> resources;
        private final Thread watchThread;

        public PackResourceListener(Path path) {
            WatchService watchService;
            try {
                watchService = path.getFileSystem().newWatchService();
            } catch (Exception ignored) {
                watchService = null;
            }

            this.path = path;
            this.watchService = watchService;
            this.watchedDirectories = ObjectSets.synchronize(new ObjectArraySet<>());
            this.ignoredPaths = ObjectSets.synchronize(new ObjectArraySet<>());
            this.resources = new Object2ObjectOpenHashMap<>();

            if (this.watchService != null) {
                this.watchThread = new Thread(this::run, "Veil File Watcher Thread " + WATCHER_ID.getAndIncrement());
                this.watchThread.start();
            } else {
                this.watchThread = null;
            }
        }

        @SuppressWarnings("unchecked")
        private void run() {
            while (true) {
                WatchKey key;
                try {
                    key = this.watchService.take();
                } catch (ClosedWatchServiceException e) {
                    return;
                } catch (Throwable t) {
                    Veil.LOGGER.error("Error waiting for file", t);
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> pathWatchEvent = (WatchEvent<Path>) event;
                    Path path = ((Path) key.watchable()).resolve(pathWatchEvent.context());
                    if (this.ignoredPaths.add(path)) {
                        VeilResource<?> resource = this.resources.get(path);
                        if (resource != null) {
                            resource.onFileSystemChange(pathWatchEvent).thenRun(() -> this.ignoredPaths.remove(path));
                        }
                    }
                }

                key.reset();
            }
        }

        public void listen(VeilResource<?> resource, Path listenPath) throws IOException {
            Path folder = listenPath.getParent();
            if (folder == null) {
                return;
            }

            this.resources.put(listenPath, resource);
            if (this.watchService != null && this.watchedDirectories.add(folder)) {
                folder.register(this.watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            }
        }

        @Override
        public void close() throws IOException {
            if (this.watchService != null) {
                this.watchService.close();
            }

            try {
                if (this.watchThread != null) {
                    this.watchThread.join();
                }
            } catch (InterruptedException e) {
                throw new IOException("Failed to stop watcher thread", e);
            }
        }

        public Path getPath() {
            return this.path;
        }
    }
}
