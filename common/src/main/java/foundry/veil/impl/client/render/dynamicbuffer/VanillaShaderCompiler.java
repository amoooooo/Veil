package foundry.veil.impl.client.render.dynamicbuffer;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.ext.ShaderInstanceExtension;
import foundry.veil.impl.client.render.shader.SimpleShaderProcessor;
import net.minecraft.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

public class VanillaShaderCompiler {

    private static final Set<String> LAST_FRAME_SHADERS = ConcurrentHashMap.newKeySet();

    private AtomicBoolean cancelled;
    private CompletableFuture<?> reloadFuture;

    public VanillaShaderCompiler() {
        this.reloadFuture = CompletableFuture.completedFuture(null);
    }

    private void compileShader(AtomicBoolean cancelled, ShaderInstance shader) {
        ShaderInstanceExtension extension = (ShaderInstanceExtension) shader;
        Collection<ResourceLocation> shaderSources = extension.veil$getShaderSources();
        VertexFormat vertexFormat = shader.getVertexFormat();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        SimpleShaderProcessor.setup(resourceManager);
        for (ResourceLocation path : shaderSources) {
            try (Reader reader = resourceManager.openAsReader(path)) {
                String source = IOUtils.toString(reader);
                if (cancelled.get()) {
                    return;
                }
                GlslPreprocessor preprocessor = new GlslPreprocessor() {
                    private final Set<String> importedPaths = Sets.newHashSet();

                    @Override
                    public String applyImport(boolean useFullPath, String directory) {
                        directory = FileUtil.normalizeResourcePath((useFullPath ? path.getPath() : "shaders/include/") + directory);
                        if (!this.importedPaths.add(directory)) {
                            return null;
                        } else {
                            ResourceLocation resourcelocation = ResourceLocation.parse(directory);

                            try {
                                String s2;
                                try (Reader reader = resourceManager.openAsReader(resourcelocation)) {
                                    s2 = IOUtils.toString(reader);
                                }

                                return s2;
                            } catch (IOException e) {
                                Veil.LOGGER.error("Could not open GLSL import {}: {}", directory, e.getMessage());
                                return "#error " + e.getMessage();
                            }
                        }
                    }
                };
                source = String.join("", preprocessor.process(source));
                if (cancelled.get()) {
                    return;
                }

                boolean vertex = path.getPath().endsWith(".vsh");
                String processed = SimpleShaderProcessor.modify(shader.getName(), path, vertexFormat, vertex ? GL_VERTEX_SHADER : GL_FRAGMENT_SHADER, source);
                RenderSystem.recordRenderCall(() -> extension.veil$recompile(vertex, processed));
            } catch (Throwable t) {
                Veil.LOGGER.error("Couldn't load vanilla shader from {}", path, t);
            }
        }
        SimpleShaderProcessor.free();
    }

    /**
     * Attempts to preload all vanilla minecraft shader files before creating the shaders on the CPU.
     *
     * @param shaders The shaders to reload
     * @return A future for when vanilla shaders have reloaded
     */
    public CompletableFuture<?> reload(Collection<ShaderInstance> shaders) {
        if (this.cancelled != null) {
            // Cancel the previous tasks and move on
            this.cancelled.set(true);
        }

        Map<String, ShaderInstance> shaderMap = new ConcurrentHashMap<>(shaders.size());
        for (ShaderInstance shader : shaders) {
            shaderMap.put(shader.getName(), shader);
        }

        AtomicBoolean cancelled = new AtomicBoolean(false);
        this.cancelled = cancelled;
        TaskScheduler scheduler = new TaskScheduler("VeilShaderCompile", Math.max(1, Runtime.getRuntime().availableProcessors() / 2), () -> {
            if (cancelled.get()) {
                return null;
            }

            for (String lastFrameShader : LAST_FRAME_SHADERS) {
                ShaderInstance shader = shaderMap.remove(lastFrameShader);
                if (shader != null) {
                    return () -> this.compileShader(cancelled, shader);
                }
            }

            Iterator<ShaderInstance> iterator = shaderMap.values().iterator();
            if (iterator.hasNext()) {
                ShaderInstance shader = iterator.next();
                iterator.remove();
                return () -> this.compileShader(cancelled, shader);
            }
            return null;
        });
        CompletableFuture<?> future = scheduler.getCompletedFuture();
        future.thenRunAsync(() -> {
            if (!cancelled.get()) {
                Veil.LOGGER.info("Compiled {} vanilla shaders", shaders.size());
            }
        }, Minecraft.getInstance());
        return this.reloadFuture = future;
    }

    public boolean isCompilingShaders() {
        return !this.reloadFuture.isDone();
    }

    public static void markRendered(String shaderInstace) {
        if (VeilRenderSystem.renderer().getVanillaShaderCompiler().isCompilingShaders()) {
            LAST_FRAME_SHADERS.add(shaderInstace);
        }
    }

    public static void clear() {
        LAST_FRAME_SHADERS.clear();
    }

    private static class TaskScheduler {

        private final int threadCount;
        private final Semaphore semaphore;

        private final CompletableFuture<?> completedFuture;
        private final Supplier<Runnable> source;
        private final Deque<Runnable> queue;
        private final AtomicBoolean running;
        private final AtomicInteger finished;

        public TaskScheduler(String name, int threadCount, Supplier<Runnable> source) {
            this.threadCount = threadCount;
            this.semaphore = new Semaphore(0);

            this.completedFuture = new CompletableFuture<>();
            this.source = source;
            this.queue = new ConcurrentLinkedDeque<>();
            this.running = new AtomicBoolean(true);
            this.finished = new AtomicInteger(0);

            for (int i = 0; i < this.threadCount; i++) {
                Thread thread = new Thread(this::run, name + "Thread#" + i);
                thread.setPriority(Math.max(0, Thread.NORM_PRIORITY - 2));
                thread.start();
            }

            for (int i = 0; i < this.threadCount; i++) {
                Runnable work = source.get();
                if (work == null) {
                    this.running.set(false);
                    this.semaphore.release(this.threadCount);
                    return;
                }

                this.queue.add(work);
                this.semaphore.release();
            }
        }

        private void run() {
            while (this.running.get()) {
                Runnable task;

                try {
                    this.semaphore.acquire();

                    // Pull off existing work from the queue, then try to populate it again
                    task = this.queue.poll();
                    if (task != null) {
                        Runnable next = this.source.get();
                        if (next == null) {
                            this.running.set(false);
                            this.semaphore.release(this.threadCount);
                        } else {
                            this.queue.add(next);
                            this.semaphore.release();
                        }
                    }
                } catch (InterruptedException ignored) {
                    continue;
                }

                if (task == null) {
                    continue;
                }

                try {
                    task.run();
                } catch (Throwable t) {
                    Veil.LOGGER.error("Error running task", t);
                }
            }

            if (this.finished.incrementAndGet() >= this.threadCount) {
                this.completedFuture.complete(null);
            }
        }

        public CompletableFuture<?> getCompletedFuture() {
            return this.completedFuture;
        }
    }
}
