package foundry.veil.impl.client.render.dynamicbuffer;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.ext.ShaderInstanceExtension;
import foundry.veil.impl.ThreadTaskScheduler;
import foundry.veil.impl.client.render.shader.SimpleShaderProcessor;
import net.minecraft.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

public class VanillaShaderCompiler {

    private static final Set<String> LAST_FRAME_SHADERS = ConcurrentHashMap.newKeySet();

    private ThreadTaskScheduler scheduler;

    public VanillaShaderCompiler() {
    }

    private void compileShader(ShaderInstance shader, int activeBuffers) {
        ShaderInstanceExtension extension = (ShaderInstanceExtension) shader;
        Collection<ResourceLocation> shaderSources = extension.veil$getShaderSources();
        VertexFormat vertexFormat = shader.getVertexFormat();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        SimpleShaderProcessor.setup(resourceManager, activeBuffers, Collections.emptySet());
        for (ResourceLocation path : shaderSources) {
            try (Reader reader = resourceManager.openAsReader(path)) {
                String source = IOUtils.toString(reader);
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

                boolean vertex = path.getPath().endsWith(".vsh");
                String processed = SimpleShaderProcessor.modify(shader.getName(), path, vertexFormat, vertex ? GL_VERTEX_SHADER : GL_FRAGMENT_SHADER, source);
                RenderSystem.recordRenderCall(() -> extension.veil$recompile(vertex, processed, activeBuffers));
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
        if (this.scheduler != null) {
            // Cancel the previous tasks and move on
            this.scheduler.cancel();
        }

        Map<String, ShaderInstance> shaderMap = new ConcurrentHashMap<>(shaders.size());
        for (ShaderInstance shader : shaders) {
            shaderMap.put(shader.getName(), shader);
        }

        int activeBuffers = VeilRenderSystem.renderer().getDynamicBufferManger().getActiveBuffers();
        ThreadTaskScheduler scheduler = new ThreadTaskScheduler("VeilVanillaShaderCompile", Math.max(1, Runtime.getRuntime().availableProcessors() / 6), () -> {
            for (String lastFrameShader : LAST_FRAME_SHADERS) {
                ShaderInstance shader = shaderMap.remove(lastFrameShader);
                if (shader != null) {
                    return () -> this.compileShader(shader, activeBuffers);
                }
            }

            Iterator<ShaderInstance> iterator = shaderMap.values().iterator();
            if (iterator.hasNext()) {
                ShaderInstance shader = iterator.next();
                iterator.remove();
                return () -> this.compileShader(shader, activeBuffers);
            }
            return null;
        });
        this.scheduler = scheduler;
        CompletableFuture<?> future = scheduler.getCompletedFuture();
        future.thenRunAsync(() -> {
            if (!scheduler.isCancelled()) {
                Veil.LOGGER.info("Compiled {} vanilla shaders", shaders.size());
            }
        }, Minecraft.getInstance());
        return future;
    }

    public boolean isCompilingShaders() {
        return this.scheduler != null && !this.scheduler.getCompletedFuture().isDone();
    }

    @ApiStatus.Internal
    public static void markRendered(String shaderInstace) {
        if (VeilRenderSystem.renderer().getVanillaShaderCompiler().isCompilingShaders()) {
            LAST_FRAME_SHADERS.add(shaderInstace);
        }
    }

    @ApiStatus.Internal
    public static void clear() {
        LAST_FRAME_SHADERS.clear();
    }
}
