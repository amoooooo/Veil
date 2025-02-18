package foundry.veil.api.client.render;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import foundry.veil.Veil;
import foundry.veil.api.client.render.ext.VeilDebug;
import foundry.veil.api.client.render.ext.VeilMultiBind;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.ext.LevelRendererExtension;
import foundry.veil.ext.VertexBufferExtension;
import foundry.veil.impl.client.imgui.VeilImGuiImpl;
import foundry.veil.impl.client.necromancer.render.NecromancerRenderDispatcher;
import foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler;
import foundry.veil.impl.client.render.pipeline.VeilBloomRenderer;
import foundry.veil.impl.client.render.pipeline.VeilShaderBlockState;
import foundry.veil.impl.client.render.pipeline.VeilShaderBufferCache;
import foundry.veil.impl.client.render.shader.program.ShaderProgramImpl;
import foundry.veil.mixin.pipeline.accessor.PipelineBufferSourceAccessor;
import foundry.veil.platform.VeilEventPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.lang.Math;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.*;

import static org.lwjgl.opengl.ARBDirectStateAccess.glCreateTextures;
import static org.lwjgl.opengl.ARBDirectStateAccess.glCreateVertexArrays;
import static org.lwjgl.opengl.ARBTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY;
import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL30C.GL_MAX_COLOR_ATTACHMENTS;
import static org.lwjgl.opengl.GL31C.GL_MAX_UNIFORM_BUFFER_BINDINGS;
import static org.lwjgl.opengl.GL43C.*;

/**
 * Additional functionality for {@link RenderSystem}.
 */
public final class VeilRenderSystem {

    /**
     * Output state for drawing into the bloom framebuffer.
     */
    public static final RenderStateShard.OutputStateShard BLOOM_SHARD = new RenderStateShard.OutputStateShard(Veil.MODID + ":bloom", VeilBloomRenderer::setupRenderState, VeilBloomRenderer::clearRenderState);

    private static final Executor RENDER_THREAD_EXECUTOR = task -> {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(task::run);
        } else {
            task.run();
        }
    };
    private static final Set<ResourceLocation> ERRORED_SHADERS = new HashSet<>();
    private static final VeilShaderBlockState UNIFORM_BLOCK_STATE = new VeilShaderBlockState();
    private static final VeilShaderBufferCache SHADER_BUFFER_CACHE = new VeilShaderBufferCache();

    private static final BooleanSupplier COMPUTE_SUPPORTED = glCapability(caps -> caps.OpenGL43 || caps.GL_ARB_compute_shader);
    private static final BooleanSupplier ATOMIC_COUNTER_SUPPORTED = glCapability(caps -> caps.OpenGL42 || caps.GL_ARB_shader_atomic_counters);
    private static final BooleanSupplier TRANSFORM_FEEDBACK_SUPPORTED = glCapability(caps -> caps.OpenGL40 || caps.GL_ARB_transform_feedback3);
    private static final BooleanSupplier MULTIBIND_SUPPORTED = glCapability(caps -> caps.OpenGL44 || caps.GL_ARB_multi_bind);
    private static final BooleanSupplier SPARSE_BUFFERS_SUPPORTED = glCapability(caps -> caps.OpenGL44 || caps.GL_ARB_sparse_buffer);
    private static final BooleanSupplier DIRECT_STATE_ACCESS_SUPPORTED = glCapability(caps -> caps.OpenGL45 || caps.GL_ARB_direct_state_access);
    private static final BooleanSupplier SEPARATE_SHADER_OBJECTS_SUPPORTED = glCapability(caps -> caps.OpenGL41|| caps.GL_ARB_separate_shader_objects);
    private static final BooleanSupplier CLEAR_TEXTURE_SUPPORTED = glCapability(caps -> caps.OpenGL44 || caps.GL_ARB_clear_texture);
    private static final BooleanSupplier COPY_IMAGE_SUPPORTED = glCapability(caps -> caps.OpenGL43 || caps.GL_ARB_copy_image);
    private static final BooleanSupplier SHADER_STORAGE_BLOCK_SUPPORTED = VeilRenderSystem.glCapability(caps -> caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object);
    private static final BooleanSupplier PROGRAM_INTERFACE_QUERY_SUPPORTED = VeilRenderSystem.glCapability(caps -> caps.OpenGL43 || caps.GL_ARB_program_interface_query);
    private static final BooleanSupplier TEXTURE_ANISOTROPY_SUPPORTED = VeilRenderSystem.glCapability(caps -> caps.OpenGL46 || caps.GL_ARB_texture_filter_anisotropic || caps.GL_EXT_texture_filter_anisotropic);
    private static final BooleanSupplier TEXTURE_MIRROR_CLAMP_TO_EDGE_SUPPORTED = VeilRenderSystem.glCapability(caps -> caps.OpenGL44 || caps.GL_ARB_texture_mirror_clamp_to_edge);
    private static final BooleanSupplier TEXTURE_CUBE_MAP_SEAMLESS_SUPPORTED = VeilRenderSystem.glCapability(caps -> caps.GL_ARB_seamless_cubemap_per_texture);
    private static final BooleanSupplier NV_DRAW_TEXTURE_SUPPORTED = VeilRenderSystem.glCapability(caps -> caps.GL_NV_draw_texture);
    private static final IntSupplier MAX_COMBINED_TEXTURE_IMAGE_UNITS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS));
    private static final IntSupplier MAX_COLOR_ATTACHMENTS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_COLOR_ATTACHMENTS));
    private static final IntSupplier MAX_SAMPLES = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_SAMPLES));
    private static final IntSupplier MAX_TRANSFORM_FEEDBACK_BUFFERS = VeilRenderSystem.glGetter(() -> TRANSFORM_FEEDBACK_SUPPORTED.getAsBoolean() ? glGetInteger(GL_MAX_TRANSFORM_FEEDBACK_BUFFERS) : 0);
    private static final IntSupplier MAX_UNIFORM_BUFFER_BINDINGS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_UNIFORM_BUFFER_BINDINGS));
    private static final IntSupplier MAX_ATOMIC_COUNTER_BUFFER_BINDINGS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS));
    private static final IntSupplier MAX_SHADER_STORAGE_BUFFER_BINDINGS = VeilRenderSystem.glGetter(() -> SHADER_STORAGE_BLOCK_SUPPORTED.getAsBoolean() ? glGetInteger(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS) : 0);
    private static final IntSupplier MAX_ARRAY_TEXTURE_LAYERS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_ARRAY_TEXTURE_LAYERS));
    private static final Supplier<Float> MAX_TEXTURE_ANISOTROPY = VeilRenderSystem.glGetter(() -> TEXTURE_ANISOTROPY_SUPPORTED.getAsBoolean() ? glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY) : 1.0F);
    private static final IntSupplier MAX_VERTEX_ATTRIBS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_VERTEX_ATTRIBS));
    private static final IntSupplier MAX_VERTEX_ATTRIB_RELATIVE_OFFSET = VeilRenderSystem.glGetter(() -> Math.max(2047, glGetInteger(GL_MAX_VERTEX_ATTRIB_RELATIVE_OFFSET)));

    private static final Supplier<VeilShaderLimits> VERTEX_SHADER_LIMITS = VeilRenderSystem.glGetter(() -> {
        GLCapabilities caps = GL.getCapabilities();
        return new VeilShaderLimits(caps,
                glGetInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS),
                glGetInteger(GL_MAX_VERTEX_UNIFORM_BLOCKS),
                glGetInteger(GL_MAX_VERTEX_ATTRIBS) * 4,
                glGetInteger(GL_MAX_VERTEX_OUTPUT_COMPONENTS),
                glGetInteger(GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS),
                GL_MAX_VERTEX_IMAGE_UNIFORMS,
                GL_MAX_VERTEX_ATOMIC_COUNTERS,
                GL_MAX_VERTEX_ATOMIC_COUNTER_BUFFERS,
                GL_MAX_VERTEX_SHADER_STORAGE_BLOCKS);
    });
    private static final Supplier<VeilShaderLimits> GL_TESS_CONTROL_SHADER_LIMITS = VeilRenderSystem.glGetter(() -> {
        GLCapabilities caps = GL.getCapabilities();
        return new VeilShaderLimits(caps,
                glGetInteger(GL_MAX_TESS_CONTROL_UNIFORM_COMPONENTS),
                glGetInteger(GL_MAX_TESS_CONTROL_UNIFORM_BLOCKS),
                glGetInteger(GL_MAX_TESS_CONTROL_INPUT_COMPONENTS),
                glGetInteger(GL_MAX_TESS_CONTROL_OUTPUT_COMPONENTS),
                glGetInteger(GL_MAX_TESS_CONTROL_TEXTURE_IMAGE_UNITS),
                GL_MAX_TESS_CONTROL_IMAGE_UNIFORMS,
                GL_MAX_TESS_CONTROL_ATOMIC_COUNTERS,
                GL_MAX_TESS_CONTROL_ATOMIC_COUNTER_BUFFERS,
                GL_MAX_TESS_CONTROL_SHADER_STORAGE_BLOCKS);
    });
    private static final Supplier<VeilShaderLimits> GL_TESS_EVALUATION_SHADER_LIMITS = VeilRenderSystem.glGetter(() -> {
        GLCapabilities caps = GL.getCapabilities();
        return new VeilShaderLimits(caps,
                glGetInteger(GL_MAX_TESS_EVALUATION_UNIFORM_COMPONENTS),
                glGetInteger(GL_MAX_TESS_EVALUATION_UNIFORM_BLOCKS),
                glGetInteger(GL_MAX_TESS_EVALUATION_INPUT_COMPONENTS),
                glGetInteger(GL_MAX_TESS_EVALUATION_OUTPUT_COMPONENTS),
                glGetInteger(GL_MAX_TESS_EVALUATION_TEXTURE_IMAGE_UNITS),
                GL_MAX_TESS_EVALUATION_IMAGE_UNIFORMS,
                GL_MAX_TESS_EVALUATION_ATOMIC_COUNTERS,
                GL_MAX_TESS_EVALUATION_ATOMIC_COUNTER_BUFFERS,
                GL_MAX_TESS_EVALUATION_SHADER_STORAGE_BLOCKS);
    });
    private static final Supplier<VeilShaderLimits> GL_GEOMETRY_SHADER_LIMITS = VeilRenderSystem.glGetter(() -> {
        GLCapabilities caps = GL.getCapabilities();
        return new VeilShaderLimits(caps,
                glGetInteger(GL_MAX_GEOMETRY_UNIFORM_COMPONENTS),
                glGetInteger(GL_MAX_GEOMETRY_UNIFORM_BLOCKS),
                glGetInteger(GL_MAX_GEOMETRY_INPUT_COMPONENTS),
                glGetInteger(GL_MAX_GEOMETRY_OUTPUT_COMPONENTS),
                glGetInteger(GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS),
                GL_MAX_GEOMETRY_IMAGE_UNIFORMS,
                GL_MAX_GEOMETRY_ATOMIC_COUNTERS,
                GL_MAX_GEOMETRY_ATOMIC_COUNTER_BUFFERS,
                GL_MAX_GEOMETRY_SHADER_STORAGE_BLOCKS);
    });
    private static final Supplier<VeilShaderLimits> GL_FRAGMENT_SHADER_LIMITS = VeilRenderSystem.glGetter(() -> {
        GLCapabilities caps = GL.getCapabilities();
        return new VeilShaderLimits(caps,
                glGetInteger(GL_MAX_FRAGMENT_UNIFORM_COMPONENTS),
                glGetInteger(GL_MAX_FRAGMENT_UNIFORM_BLOCKS),
                glGetInteger(GL_MAX_FRAGMENT_INPUT_COMPONENTS),
                glGetInteger(GL_MAX_DRAW_BUFFERS) * 4,
                glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS),
                GL_MAX_FRAGMENT_IMAGE_UNIFORMS,
                GL_MAX_FRAGMENT_ATOMIC_COUNTERS,
                GL_MAX_FRAGMENT_ATOMIC_COUNTER_BUFFERS,
                GL_MAX_FRAGMENT_SHADER_STORAGE_BLOCKS);
    });
    private static final Supplier<VeilShaderLimits> GL_COMPUTE_SHADER_LIMITS = VeilRenderSystem.glGetter(() -> {
        GLCapabilities caps = GL.getCapabilities();
        return new VeilShaderLimits(caps,
                glGetInteger(GL_MAX_COMPUTE_UNIFORM_COMPONENTS),
                glGetInteger(GL_MAX_COMPUTE_UNIFORM_BLOCKS),
                0,
                0,
                glGetInteger(GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS),
                GL_MAX_COMPUTE_IMAGE_UNIFORMS,
                GL_MAX_COMPUTE_ATOMIC_COUNTERS,
                GL_MAX_COMPUTE_ATOMIC_COUNTER_BUFFERS,
                GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS);
    });

    private static final Supplier<Vector2ic> MAX_FRAMEBUFFER_SIZE = Suppliers.memoize(() -> {
        RenderSystem.assertOnRenderThreadOrInit();
        if (!GL.getCapabilities().OpenGL43) {
            return new Vector2i(Integer.MAX_VALUE);
        }
        int width = glGetInteger(GL_MAX_FRAMEBUFFER_WIDTH);
        int height = glGetInteger(GL_MAX_FRAMEBUFFER_HEIGHT);
        return new Vector2i(width, height);
    });
    private static final Supplier<Vector3ic> MAX_COMPUTE_WORK_GROUP_COUNT = Suppliers.memoize(() -> {
        RenderSystem.assertOnRenderThreadOrInit();
        if (!COMPUTE_SUPPORTED.getAsBoolean()) {
            return new Vector3i();
        }

        int width = glGetIntegeri(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0);
        int height = glGetIntegeri(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1);
        int depth = glGetIntegeri(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2);
        return new Vector3i(width, height, depth);
    });
    private static final Supplier<Vector3ic> MAX_COMPUTE_WORK_GROUP_SIZE = Suppliers.memoize(() -> {
        RenderSystem.assertOnRenderThreadOrInit();
        if (!COMPUTE_SUPPORTED.getAsBoolean()) {
            return new Vector3i();
        }

        int width = glGetIntegeri(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0);
        int height = glGetIntegeri(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1);
        int depth = glGetIntegeri(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2);
        return new Vector3i(width, height, depth);
    });
    private static final IntSupplier MAX_COMPUTE_WORK_GROUP_INVOCATIONS = VeilRenderSystem.glGetter(() -> COMPUTE_SUPPORTED.getAsBoolean() ? glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS) : 0);
    private static final LongSupplier MAX_UNIFORM_BLOCK_SIZE = VeilRenderSystem.glGetter(() -> glGetInteger64(GL_MAX_UNIFORM_BLOCK_SIZE));
    private static final IntSupplier UNIFORM_BUFFER_OFFSET_ALIGNMENT = VeilRenderSystem.glGetter(() -> glGetInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT));
    private static final LongSupplier MAX_SHADER_STORAGE_BLOCK_SIZE = VeilRenderSystem.glGetter(() -> SHADER_STORAGE_BLOCK_SUPPORTED.getAsBoolean() ? glGetInteger64(GL_MAX_SHADER_STORAGE_BLOCK_SIZE) : 0);

    private static final Vector3f LIGHT0_DIRECTION = new Vector3f();
    private static final Vector3f LIGHT1_DIRECTION = new Vector3f();
    private static final Vector3f CAMERA_BOB_OFFSET = new Vector3f();

    private static VeilRenderer renderer;
    private static ResourceLocation shaderLocation;
    private static int screenQuadVao;
    private static IntBuffer emptySamplers;

    private VeilRenderSystem() {
    }

    private static BooleanSupplier glCapability(Function<GLCapabilities, Boolean> delegate) {
        return new BooleanSupplier() {
            private boolean value;
            private boolean initialized;

            @Override
            public boolean getAsBoolean() {
                RenderSystem.assertOnRenderThreadOrInit();
                if (!this.initialized) {
                    this.initialized = true;
                    return this.value = delegate.apply(GL.getCapabilities());
                }
                return this.value;
            }
        };
    }

    private static IntSupplier glGetter(IntSupplier delegate) {
        return new IntSupplier() {
            private int value = Integer.MAX_VALUE;

            @Override
            public int getAsInt() {
                RenderSystem.assertOnRenderThreadOrInit();
                if (this.value == Integer.MAX_VALUE) {
                    return this.value = delegate.getAsInt();
                }
                return this.value;
            }
        };
    }

    private static LongSupplier glGetter(LongSupplier delegate) {
        return new LongSupplier() {
            private long value = Long.MAX_VALUE;

            @Override
            public long getAsLong() {
                RenderSystem.assertOnRenderThreadOrInit();
                if (this.value == Long.MAX_VALUE) {
                    return this.value = delegate.getAsLong();
                }
                return this.value;
            }
        };
    }

    private static <T> Supplier<T> glGetter(Supplier<T> delegate) {
        return new Supplier<T>() {
            private T value = null;

            @Override
            public T get() {
                RenderSystem.assertOnRenderThreadOrInit();
                if (this.value == null) {
                    return this.value = delegate.get();
                }
                return this.value;
            }
        };
    }

    @ApiStatus.Internal
    public static void bootstrap() {
        VeilEventPlatform.INSTANCE.onVeilShaderCompile((shaderManager, updatedPrograms) -> {
            UNIFORM_BLOCK_STATE.onShaderCompile();
            SHADER_BUFFER_CACHE.onShaderCompile(updatedPrograms);
            ERRORED_SHADERS.clear();
        });
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage((stage, levelRenderer, bufferSource, matrixStack, frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum) -> {
            if (stage == VeilRenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
                NecromancerRenderDispatcher.begin();
            } else if (stage == VeilRenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                NecromancerRenderDispatcher.end();
            }
        });
    }

    @ApiStatus.Internal
    public static void init() {
        Minecraft client = Minecraft.getInstance();
        if (!(client.getResourceManager() instanceof ReloadableResourceManager resourceManager)) {
            throw new IllegalStateException("Client resource manager is " + client.getResourceManager().getClass());
        }

        renderer = new VeilRenderer(resourceManager, client.getWindow());
        VeilImGuiImpl.init(client.getWindow().getWindow());
        screenQuadVao = directStateAccessSupported() ? glCreateVertexArrays() : glGenVertexArrays();
        VeilDebug.get().objectLabel(GL_VERTEX_ARRAY, screenQuadVao, "Screen Quad Vertex Array");
        emptySamplers = MemoryUtil.memCallocInt(maxCombinedTextureUnits());
    }

    /**
     * Binds the specified texture ids to sequential texture units and invalidates the GLStateManager.
     *
     * @param first    The first unit to bind to
     * @param textures The textures to bind
     */
    public static void bindTextures(int first, IntBuffer textures) {
        VeilMultiBind.get().bindTextures(first, textures);
    }

    /**
     * Binds the specified texture ids to sequential texture units and invalidates the GLStateManager.
     *
     * @param first    The first unit to bind to
     * @param textures The textures to bind
     */
    public static void bindTextures(int first, int... textures) {
        VeilMultiBind.get().bindTextures(first, textures);
    }

    /**
     * Binds the specified sampler ids to sequential texture units.
     *
     * @param first    The first unit to bind to
     * @param textures The samplers to bind
     */
    public static void bindSamplers(int first, IntBuffer textures) {
        VeilMultiBind.get().bindSamplers(first, textures);
    }

    /**
     * Binds the specified sampler ids to sequential texture units.
     *
     * @param first    The first unit to bind to
     * @param textures The samplers to bind
     */
    public static void bindSamplers(int first, int... textures) {
        VeilMultiBind.get().bindSamplers(first, textures);
    }

    /**
     * Unbinds the specified number of sampler from sequential texture units.
     *
     * @param first The first unit to unbind from
     * @param count The number of samplers to unbind
     */
    public static void unbindSamplers(int first, int count) {
        VeilMultiBind.get().bindSamplers(first, emptySamplers.limit(count));
    }

    /**
     * Draws a quad onto the full screen.
     */
    public static void drawScreenQuad() {
        GlStateManager._glBindVertexArray(screenQuadVao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 3);
        VertexBuffer.unbind();
    }

    /**
     * Sets the shader instance to be a reference to the shader manager.
     *
     * @param shader The name of the shader to use
     * @return The Veil shader instance applied or <code>null</code> if there was an error
     */
    public static @Nullable ShaderProgram setShader(ResourceLocation shader) {
        ShaderManager shaderManager = renderer.getShaderManager();
        VeilRenderSystem.shaderLocation = shader;
        return VeilRenderSystem.setShader(() -> shaderManager.getShader(shader));
    }

    /**
     * Sets the shader instance to a specific instance of a shader. {@link #setShader(ResourceLocation)} should be used in most cases.
     *
     * @param shader The shader instance to use
     * @return The Veil shader instance applied or <code>null</code> if there was an error
     */
    public static @Nullable ShaderProgram setShader(@Nullable ShaderProgram shader) {
        VeilRenderSystem.shaderLocation = shader != null ? shader.getName() : null;
        return VeilRenderSystem.setShader(() -> shader);
    }

    /**
     * Sets the shader instance to a specific instance reference of a shader. {@link #setShader(ResourceLocation)} should be used in most cases.
     *
     * @param shader The reference to the shader to use
     * @return The Veil shader instance applied or <code>null</code> if there was an error
     */
    public static @Nullable ShaderProgram setShader(Supplier<ShaderProgram> shader) {
        RenderSystem.setShader(() -> {
            ShaderProgram program = shader.get();
            return program != null ? program.toShaderInstance() : null;
        });

        ShaderProgram value = VeilRenderSystem.getShader();
        if (value == null && VeilRenderSystem.shaderLocation != null && ERRORED_SHADERS.add(VeilRenderSystem.shaderLocation)) {
            Veil.LOGGER.error("Failed to apply shader: {}", VeilRenderSystem.shaderLocation);
        }
        return value;
    }

    /**
     * Draws instances of the specified vertex buffer.
     *
     * @param vbo       The vertex buffer to draw
     * @param instances The number of instances to draw
     * @see <a target="_blank" href="http://docs.gl/gl4/glDrawArraysInstanced">Reference Page</a>
     */
    public static void drawInstanced(VertexBuffer vbo, int instances) {
        ((VertexBufferExtension) vbo).veil$drawInstanced(instances);
    }

    /**
     * Draws indirect instances of the specified vertex buffer.
     *
     * @param vbo       The vertex buffer to draw
     * @param indirect  A pointer into the currently bound {@link GL40C#GL_DRAW_INDIRECT_BUFFER} or the address of a struct containing draw data
     * @param drawCount The number of primitives to draw
     * @param stride    The offset between indirect elements
     * @see <a target="_blank" href="http://docs.gl/gl4/glMultiDrawElementsIndirect">Reference Page</a>
     */
    public static void drawIndirect(VertexBuffer vbo, long indirect, int drawCount, int stride) {
        ((VertexBufferExtension) vbo).veil$drawIndirect(indirect, drawCount, stride);
    }

    /**
     * Finishes the last batch of the specified buffer builder if it has the same name.
     *
     * @param source The source to end the buffer for
     * @param name   The name of the buffer to end
     */
    public static void endLastBatch(MultiBufferSource.BufferSource source, String name) {
        if (source instanceof PipelineBufferSourceAccessor accessor) {
            RenderType renderType = accessor.getLastSharedType();
            if (renderType != null && VeilRenderType.getName(renderType).equals(name)) {
                source.endLastBatch();
            }
        }
    }

    /**
     * Finishes the last batch of the specified buffer builder if it is the same render type.
     *
     * @param source     The source to end the buffer for
     * @param renderType The render type to end
     */
    public static void endLastBatch(MultiBufferSource.BufferSource source, RenderType renderType) {
        if (source instanceof PipelineBufferSourceAccessor accessor) {
            RenderType lastSharedType = accessor.getLastSharedType();
            if (lastSharedType != null && lastSharedType.equals(renderType)) {
                source.endLastBatch();
            }
        }
    }

    /**
     * Rebuilds all chunks in view without deleting the old chunks.
     */
    public static void rebuildChunks() {
        ((LevelRendererExtension) Minecraft.getInstance().levelRenderer).veil$markChunksDirty();
    }

    /**
     * Consumes all OpenGL errors and prints them to console.
     *
     * @param glCall The name of the OpenGL call made for extra logging or <code>null</code> to not include
     */
    public static void printGlErrors(@Nullable String glCall) {
        while (true) {
            int error = GlStateManager._getError();
            if (error == GL_NO_ERROR) {
                break;
            }

            if (glCall != null) {
                Veil.LOGGER.error("[OpenGL Error] '{}' 0x{}", glCall, Integer.toHexString(error).toUpperCase(Locale.ROOT));
            } else {
                Veil.LOGGER.error("[OpenGL Error] 0x{}", Integer.toHexString(error).toUpperCase(Locale.ROOT));
            }
        }
    }

    /**
     * Retrieves the number of indices in the specified vertex buffer.
     *
     * @param vbo The vertex buffer to query
     * @return The number of indices in the buffer
     */
    public static int getIndexCount(VertexBuffer vbo) {
        return ((VertexBufferExtension) vbo).veil$getIndexCount();
    }

    /**
     * @return Whether compute shaders are supported
     */
    public static boolean computeSupported() {
        return VeilRenderSystem.COMPUTE_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether atomic counters in shaders are supported
     */
    public static boolean atomicCounterSupported() {
        return VeilRenderSystem.ATOMIC_COUNTER_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether transform feedback from shaders is supported
     */
    public static boolean transformFeedbackSupported() {
        return VeilRenderSystem.TRANSFORM_FEEDBACK_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBMultiBind} is supported
     */
    public static boolean multibindSupported() {
        return VeilRenderSystem.MULTIBIND_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBSparseBuffer} is supported
     */
    public static boolean sparseBuffersSupported() {
        return VeilRenderSystem.SPARSE_BUFFERS_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBDirectStateAccess} is supported
     */
    public static boolean directStateAccessSupported() {
        return VeilRenderSystem.DIRECT_STATE_ACCESS_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBSeparateShaderObjects} is supported
     */
    public static boolean separateShaderObjectsSupported() {
        return VeilRenderSystem.SEPARATE_SHADER_OBJECTS_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBClearTexture} is supported
     */
    public static boolean clearTextureSupported() {
        return VeilRenderSystem.CLEAR_TEXTURE_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBCopyImage} is supported
     */
    public static boolean copyImageSupported() {
        return VeilRenderSystem.COPY_IMAGE_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBShaderStorageBufferObject} is supported
     */
    public static boolean shaderStorageBufferSupported() {
        return VeilRenderSystem.SHADER_STORAGE_BLOCK_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBProgramInterfaceQuery} is supported
     */
    public static boolean programInterfaceQuerySupported() {
        return VeilRenderSystem.PROGRAM_INTERFACE_QUERY_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBTextureFilterAnisotropic} is supported
     */
    public static boolean textureAnisotropySupported() {
        return VeilRenderSystem.TEXTURE_ANISOTROPY_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBTextureMirrorClampToEdge} is supported
     */
    public static boolean textureMirrorClampToEdgeSupported() {
        return VeilRenderSystem.TEXTURE_MIRROR_CLAMP_TO_EDGE_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link ARBSeamlessCubemapPerTexture} is supported
     */
    public static boolean textureCubeMapSeamlessSupported() {
        return VeilRenderSystem.TEXTURE_CUBE_MAP_SEAMLESS_SUPPORTED.getAsBoolean();
    }

    /**
     * @return Whether {@link NVDrawTexture} is supported
     */
    public static boolean nvDrawTextureSupported() {
        return VeilRenderSystem.NV_DRAW_TEXTURE_SUPPORTED.getAsBoolean();
    }

    /**
     * @return The GL maximum number of texture units that can be bound
     */
    public static int maxCombinedTextureUnits() {
        return VeilRenderSystem.MAX_COMBINED_TEXTURE_IMAGE_UNITS.getAsInt();
    }

    /**
     * @return The GL maximum amount of color attachments a framebuffer can have
     */
    public static int maxColorAttachments() {
        return VeilRenderSystem.MAX_COLOR_ATTACHMENTS.getAsInt();
    }

    /**
     * @return The GL maximum amount of samples a render buffer can have
     */
    public static int maxSamples() {
        return VeilRenderSystem.MAX_SAMPLES.getAsInt();
    }

    /**
     * Retrieves the maximum bindings for the specified buffer binding.
     *
     * @param target The target to query the maximum bindings of
     * @return The GL maximum amount of buffer bindings available
     */
    public static int maxTargetBindings(int target) {
        return switch (target) {
            case GL_TRANSFORM_FEEDBACK_BUFFER -> maxTransformFeedbackBindings();
            case GL_UNIFORM_BUFFER -> maxUniformBuffersBindings();
            case GL_ATOMIC_COUNTER_BUFFER -> maxAtomicCounterBufferBindings();
            case GL_SHADER_STORAGE_BUFFER -> maxShaderStorageBufferBindings();
            default ->
                    throw new IllegalArgumentException("Invalid Target: 0x" + Integer.toHexString(target).toUpperCase(Locale.ROOT));
        };
    }

    /**
     * Retrieves the maximum limits for the specified shader type.
     *
     * @param shader The shader to query the limits for
     * @return The GL limits available
     */
    public static VeilShaderLimits shaderLimits(int shader) {
        return switch (shader) {
            case GL_VERTEX_SHADER -> VERTEX_SHADER_LIMITS.get();
            case GL_TESS_CONTROL_SHADER -> GL_TESS_CONTROL_SHADER_LIMITS.get();
            case GL_TESS_EVALUATION_SHADER -> GL_TESS_EVALUATION_SHADER_LIMITS.get();
            case GL_GEOMETRY_SHADER -> GL_GEOMETRY_SHADER_LIMITS.get();
            case GL_FRAGMENT_SHADER -> GL_FRAGMENT_SHADER_LIMITS.get();
            case GL_COMPUTE_SHADER -> GL_COMPUTE_SHADER_LIMITS.get();
            default ->
                    throw new IllegalArgumentException("Invalid Shader Type: 0x" + Integer.toHexString(shader).toUpperCase(Locale.ROOT));
        };
    }

    /**
     * @return The GL maximum number of transform feedback buffers bindings available
     */
    public static int maxTransformFeedbackBindings() {
        return VeilRenderSystem.MAX_TRANSFORM_FEEDBACK_BUFFERS.getAsInt();
    }

    /**
     * @return The GL maximum number of uniform buffers bindings available
     */
    public static int maxUniformBuffersBindings() {
        return VeilRenderSystem.MAX_UNIFORM_BUFFER_BINDINGS.getAsInt();
    }

    /**
     * @return The GL maximum number of atomic counter buffers bindings available
     */
    public static int maxAtomicCounterBufferBindings() {
        return VeilRenderSystem.MAX_ATOMIC_COUNTER_BUFFER_BINDINGS.getAsInt();
    }

    /**
     * @return The GL maximum number of shader storage buffers bindings available
     */
    public static int maxShaderStorageBufferBindings() {
        return VeilRenderSystem.MAX_SHADER_STORAGE_BUFFER_BINDINGS.getAsInt();
    }

    /**
     * @return The GL maximum number of array texture layers available
     */
    public static int maxArrayTextureLayers() {
        return VeilRenderSystem.MAX_ARRAY_TEXTURE_LAYERS.getAsInt();
    }

    /**
     * @return The GL maximum texture anisotropy value
     */
    public static float maxTextureAnisotropy() {
        return VeilRenderSystem.MAX_TEXTURE_ANISOTROPY.get();
    }

    /**
     * @return The GL maximum number of vertex attributes available
     */
    public static int maxVertexAttributes() {
        return VeilRenderSystem.MAX_VERTEX_ATTRIBS.getAsInt();
    }

    /**
     * @return The GL maximum offset of vertex attribute relative offsets
     */
    public static int maxVertexAttributeRelativeOffset() {
        return VeilRenderSystem.MAX_VERTEX_ATTRIB_RELATIVE_OFFSET.getAsInt();
    }

    /**
     * @return The GL maximum width of framebuffers
     */
    public static int maxFramebufferWidth() {
        return VeilRenderSystem.MAX_FRAMEBUFFER_SIZE.get().x();
    }

    /**
     * @return The GL maximum width of framebuffers
     */
    public static int maxFramebufferHeight() {
        return VeilRenderSystem.MAX_FRAMEBUFFER_SIZE.get().y();
    }

    /**
     * @return The GL maximum number of work groups in the X
     */
    public static int maxComputeWorkGroupCountX() {
        return VeilRenderSystem.MAX_COMPUTE_WORK_GROUP_COUNT.get().y();
    }

    /**
     * @return The GL maximum number of work groups in the Y
     */
    public static int maxComputeWorkGroupCountY() {
        return VeilRenderSystem.MAX_COMPUTE_WORK_GROUP_COUNT.get().y();
    }

    /**
     * @return The GL maximum number of work groups in the Z
     */
    public static int maxComputeWorkGroupCountZ() {
        return VeilRenderSystem.MAX_COMPUTE_WORK_GROUP_COUNT.get().y();
    }

    /**
     * @return The GL maximum number of local work groups in the X
     */
    public static int maxComputeWorkGroupSizeX() {
        return VeilRenderSystem.MAX_COMPUTE_WORK_GROUP_SIZE.get().y();
    }

    /**
     * @return The GL maximum number of local work groups in the Y
     */
    public static int maxComputeWorkGroupSizeY() {
        return VeilRenderSystem.MAX_COMPUTE_WORK_GROUP_SIZE.get().y();
    }

    /**
     * @return The GL maximum number of local work groups in the Z
     */
    public static int maxComputeWorkGroupSizeZ() {
        return VeilRenderSystem.MAX_COMPUTE_WORK_GROUP_SIZE.get().y();
    }

    /**
     * @return The GL maximum number of total compute shader invocations
     */
    public static int maxComputeWorkGroupInvocations() {
        return VeilRenderSystem.MAX_COMPUTE_WORK_GROUP_INVOCATIONS.getAsInt();
    }

    /**
     * @return The GL maximum size of uniform buffers
     */
    public static long maxUniformBufferSize() {
        return VeilRenderSystem.MAX_UNIFORM_BLOCK_SIZE.getAsLong();
    }

    /**
     * @return The GL offset byte alignment requirement of uniform buffers
     */
    public static int uniformBufferAlignment() {
        return VeilRenderSystem.UNIFORM_BUFFER_OFFSET_ALIGNMENT.getAsInt();
    }

    /**
     * @return The GL maximum size of shader storage buffers
     */
    public static long maxShaderStorageBufferSize() {
        return VeilRenderSystem.MAX_SHADER_STORAGE_BLOCK_SIZE.getAsLong();
    }

    /**
     * <p>Binds the specified block into the next available binding spot
     * and updates all shaders if the binding index has changed.</p>
     * <p><b>Make sure this is called before trying to use the block on this frame as it may have been overwritten.</b></p>
     * <p>This binds the block and assigns it to shader values.</p>
     *
     * @param name  The name of the block in shader code
     * @param block The block to bind
     */
    public static void bind(CharSequence name, ShaderBlock<?> block) {
        RenderSystem.assertOnRenderThreadOrInit();
        UNIFORM_BLOCK_STATE.bind(name, block);
    }

    /**
     * <p>Binds the specified block into the next available binding spot
     * and updates all shaders if the binding index has changed.</p>
     * <p><b>Make sure this is called before trying to use the block on this frame as it may have been overwritten.</b></p>
     * <p>This binds the block and assigns it to shader values.</p>
     *
     * @param layout The layout of the buffer to bind
     * @throws IllegalArgumentException If the layout is not registered
     */
    public static void bind(VeilShaderBufferLayout<?> layout) throws IllegalArgumentException {
        RenderSystem.assertOnRenderThreadOrInit();
        SHADER_BUFFER_CACHE.bind(layout);
    }

    /**
     * Unbinds the specified block and frees the binding it occupied.
     * It isn't strictly necessary to unbind blocks, but they should not be referenced anymore after being deleted.
     *
     * @param block The block to unbind
     */
    public static void unbind(ShaderBlock<?> block) {
        RenderSystem.assertOnRenderThreadOrInit();
        UNIFORM_BLOCK_STATE.unbind(block);
    }

    /**
     * Unbinds the specified block and frees the binding it occupied.
     * It isn't strictly necessary to unbind blocks, but they should not be referenced anymore after being deleted.
     *
     * @param layout The layout of the buffer to unbind
     * @throws IllegalArgumentException If the layout is not registered
     */
    public static void unbind(VeilShaderBufferLayout<?> layout) throws IllegalArgumentException {
        RenderSystem.assertOnRenderThreadOrInit();
        SHADER_BUFFER_CACHE.unbind(layout);
    }

    /**
     * Retrieves the registered block for the specified layout. Make sure the layout is registered.
     *
     * @param layout The layout to retrieve the block for
     * @param <T>    The type of data the block encodes
     * @return The block created or <code>null</code> if no shaders reference the block. <code>#buffer namespace:path</code>
     * @throws IllegalArgumentException If the layout is not registered
     */
    public static <T> @Nullable ShaderBlock<T> getBlock(VeilShaderBufferLayout<T> layout) throws IllegalArgumentException {
        RenderSystem.assertOnRenderThreadOrInit();
        return SHADER_BUFFER_CACHE.getBlock(layout);
    }

    /**
     * Binds the specified vertex array and invalidates the vanilla MC immediate buffer state.
     *
     * @param vao The vao to bind
     */
    public static void bindVertexArray(int vao) {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(vao);
    }

    public static int getBoundTexture(int target) {
        return switch (target) {
            case GL_TEXTURE_1D -> glGetInteger(GL_TEXTURE_BINDING_1D);
            case GL_TEXTURE_2D -> glGetInteger(GL_TEXTURE_BINDING_2D);
            case GL_TEXTURE_1D_ARRAY -> glGetInteger(GL_TEXTURE_BINDING_1D_ARRAY);
            case GL_TEXTURE_RECTANGLE -> glGetInteger(GL_TEXTURE_BINDING_RECTANGLE);
            case GL_TEXTURE_CUBE_MAP -> glGetInteger(GL_TEXTURE_BINDING_CUBE_MAP);
            case GL_TEXTURE_3D -> glGetInteger(GL_TEXTURE_BINDING_3D);
            case GL_TEXTURE_2D_ARRAY -> glGetInteger(GL_TEXTURE_BINDING_2D_ARRAY);
            case GL_TEXTURE_CUBE_MAP_ARRAY -> glGetInteger(GL_TEXTURE_BINDING_CUBE_MAP_ARRAY);
            case GL_TEXTURE_BUFFER -> glGetInteger(GL_TEXTURE_BINDING_BUFFER);
            case GL_TEXTURE_2D_MULTISAMPLE -> glGetInteger(GL_TEXTURE_BINDING_2D_MULTISAMPLE);
            case GL_TEXTURE_2D_MULTISAMPLE_ARRAY -> glGetInteger(GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY);
            default -> throw new IllegalStateException("Not a texture target: " + target);
        };
    }

    /**
     * Creates a new texture for the specified target when using direct state access,
     * otherwise calls the regular {@link GL11C#glGenTextures()}.
     *
     * @param target The target to create the texture for
     * @return A new texture object with its state initialized if {@link #directStateAccessSupported()} is <code>true</code>
     */
    public static int createTextures(int target) {
        if (directStateAccessSupported()) {
            return glCreateTextures(target);
        }
        return glGenTextures();
    }

    /**
     * Creates new textures for the specified target when using direct state access,
     * otherwise calls the regular {@link GL11C#glGenTextures()}.
     *
     * @param target   The target to create the texture for
     * @param textures The array to fill with new textures
     */
    public static void createTextures(int target, int[] textures) {
        if (directStateAccessSupported()) {
            glCreateTextures(target, textures);
            return;
        }
        glGenTextures(textures);
    }

    /**
     * Creates new textures for the specified target when using direct state access,
     * otherwise calls the regular {@link GL11C#glGenTextures()}.
     *
     * @param target   The target to create the texture for
     * @param textures The array to fill with new textures
     */
    public static void createTextures(int target, IntBuffer textures) {
        if (directStateAccessSupported()) {
            glCreateTextures(target, textures);
            return;
        }
        glGenTextures(textures);
    }

    /**
     * @return The veil renderer instance
     */
    public static VeilRenderer renderer() {
        return renderer;
    }

    /**
     * @return An executor for the main render thread
     */
    public static Executor renderThreadExecutor() {
        return RENDER_THREAD_EXECUTOR;
    }

    /**
     * @return The actual shader reference to use while rendering or <code>null</code> if no shader is selected or the selected shader is from Vanilla Minecraft
     */
    public static @Nullable ShaderProgram getShader() {
        ShaderInstance shader = RenderSystem.getShader();
        return shader instanceof ShaderProgramImpl.Wrapper wrapper ? wrapper.program() : null;
    }

    /**
     * @return The position of the first light
     */
    public static Vector3fc getLight0Direction() {
        return LIGHT0_DIRECTION;
    }

    /**
     * @return The position of the second light
     */

    public static Vector3fc getLight1Direction() {
        return LIGHT1_DIRECTION;
    }

    /**
     * @return The camera position offset from view bobbing
     */
    public static Vector3fc getCameraBobOffset() {
        return CAMERA_BOB_OFFSET;
    }

    /**
     * @return Whether ImGui can be used
     */
    public static boolean hasImGui() {
        return VeilImGuiImpl.get() instanceof VeilImGuiImpl;
    }

    /**
     * @return The culling frustum for the renderer
     */
    public static CullFrustum getCullingFrustum() {
        return ((LevelRendererExtension) Minecraft.getInstance().levelRenderer).veil$getCullFrustum();
    }

    // Internal

    @ApiStatus.Internal
    public static void beginFrame() {
        VeilImGuiImpl.get().beginFrame();

        SHADER_BUFFER_CACHE.bind();
    }

    @ApiStatus.Internal
    public static void endFrame() {
        VeilImGuiImpl.get().endFrame();

        if (Veil.platform().hasErrors()) {
            return;
        }

        renderer.endFrame();

        UNIFORM_BLOCK_STATE.clearUsedBindings();
        VanillaShaderCompiler.clear();
    }

    @ApiStatus.Internal
    public static void clearShaderBlocks() {
        SHADER_BUFFER_CACHE.unbindPacked();
    }

    @ApiStatus.Internal
    public static void shaderUpdate() {
        VeilRenderSystem.shaderLocation = null;
    }

    @ApiStatus.Internal
    public static void resize(int width, int height) {
        if (renderer != null) {
            renderer.resize(width, height);
        }
    }

    @ApiStatus.Internal
    public static void close() {
        if (VeilImGuiImpl.get() instanceof NativeResource resource) {
            resource.free();
        }
        if (renderer != null) {
            renderer.free();
        }
        glDeleteVertexArrays(screenQuadVao);
        MemoryUtil.memFree(emptySamplers);
        SHADER_BUFFER_CACHE.free();
    }

    @ApiStatus.Internal
    public static void renderPost(@Nullable VeilRenderLevelStageEvent.Stage stage) {
        if (stage == VeilRenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || stage == VeilRenderLevelStageEvent.Stage.AFTER_LEVEL) {
            VeilBloomRenderer.flush();
        }
        renderer.getPostProcessingManager().runDefaultPipeline(stage);
    }

    @ApiStatus.Internal
    public static void setShaderLights(Vector3fc light0, Vector3fc light1) {
        LIGHT0_DIRECTION.set(light0);
        LIGHT1_DIRECTION.set(light1);
    }

    @ApiStatus.Internal
    public static void setCameraBobOffset(Vector3fc offset) {
        CAMERA_BOB_OFFSET.set(offset);
    }

    @ApiStatus.Internal
    public static boolean drawLights(ProfilerFiller profiler, CullFrustum cullFrustum) {
        FramebufferManager framebufferManager = renderer.getFramebufferManager();
        AdvancedFbo lightFbo = framebufferManager.getFramebuffer(VeilFramebuffers.LIGHT);
        if (lightFbo == null) {
            AdvancedFbo.unbind();
            return false;
        }

        LightRenderer lightRenderer = renderer.getLightRenderer();
        profiler.push("setup_lights");
        lightRenderer.setup(cullFrustum);
        profiler.popPush("draw_lights");
        boolean rendered = lightRenderer.render(lightFbo);
        profiler.pop();
        return rendered;
    }

    @ApiStatus.Internal
    public static void compositeLights(ProfilerFiller profiler) {
        // Only run the post pipeline if there are lights to display
        PostProcessingManager postProcessingManager = renderer.getPostProcessingManager();
        PostPipeline compositePipeline = postProcessingManager.getPipeline(VeilRenderer.COMPOSITE);
        if (compositePipeline != null) {
            profiler.push("composite_lights");
            postProcessingManager.runPipeline(compositePipeline);
            profiler.pop();
        }
    }

    @ApiStatus.Internal
    public static void clearLevel() {
        NecromancerRenderDispatcher.delete();
    }
}
