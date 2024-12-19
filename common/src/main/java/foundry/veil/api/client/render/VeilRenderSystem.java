package foundry.veil.api.client.render;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.Veil;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.opencl.VeilOpenCL;
import foundry.veil.ext.LevelRendererExtension;
import foundry.veil.ext.VertexBufferExtension;
import foundry.veil.impl.client.imgui.VeilImGuiImpl;
import foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler;
import foundry.veil.impl.client.render.pipeline.VeilUniformBlockState;
import foundry.veil.impl.client.render.shader.ShaderProgramImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.NativeResource;

import java.lang.Math;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30C.GL_MAX_COLOR_ATTACHMENTS;
import static org.lwjgl.opengl.GL31C.GL_MAX_UNIFORM_BUFFER_BINDINGS;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.opengl.GL44C.glBindTextures;

/**
 * Additional functionality for {@link RenderSystem}.
 */
public final class VeilRenderSystem {

    private static final Executor RENDER_THREAD_EXECUTOR = task -> {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(task::run);
        } else {
            task.run();
        }
    };
    private static final Set<ResourceLocation> ERRORED_SHADERS = new HashSet<>();
    private static final VeilUniformBlockState UNIFORM_BLOCK_STATE = new VeilUniformBlockState();

    private static final BooleanSupplier COMPUTE_SUPPORTED = glCapability(caps -> caps.OpenGL43 || caps.GL_ARB_compute_shader);
    private static final BooleanSupplier ATOMIC_COUNTER_SUPPORTED = glCapability(caps -> caps.OpenGL42 || caps.GL_ARB_shader_atomic_counters);
    private static final BooleanSupplier TRANSFORM_FEEDBACK_SUPPORTED = glCapability(caps -> caps.OpenGL40 || caps.GL_ARB_transform_feedback3);
    private static final BooleanSupplier TEXTURE_MULTIBIND_SUPPORTED = glCapability(caps -> caps.OpenGL44 || caps.glBindTextures != 0L);
    private static final BooleanSupplier SPARSE_BUFFERS_SUPPORTED = glCapability(caps -> caps.OpenGL44 || caps.GL_ARB_sparse_buffer);
    private static final BooleanSupplier DIRECT_STATE_ACCESS_SUPPORTED = glCapability(caps -> caps.OpenGL45 || caps.GL_ARB_direct_state_access);
    private static final IntSupplier MAX_COMBINED_TEXTURE_IMAGE_UNITS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS));
    private static final IntSupplier MAX_COLOR_ATTACHMENTS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_COLOR_ATTACHMENTS));
    private static final IntSupplier MAX_SAMPLES = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_SAMPLES));
    private static final IntSupplier MAX_TRANSFORM_FEEDBACK_BUFFERS = VeilRenderSystem.glGetter(() -> TRANSFORM_FEEDBACK_SUPPORTED.getAsBoolean() ? glGetInteger(GL_MAX_TRANSFORM_FEEDBACK_BUFFERS) : 0);
    private static final IntSupplier MAX_UNIFORM_BUFFER_BINDINGS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_UNIFORM_BUFFER_BINDINGS));
    private static final IntSupplier MAX_ATOMIC_COUNTER_BUFFER_BINDINGS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS));
    private static final IntSupplier MAX_SHADER_STORAGE_BUFFER_BINDINGS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS));
    private static final IntSupplier MAX_ARRAY_TEXTURE_LAYERS = VeilRenderSystem.glGetter(() -> glGetInteger(GL_MAX_ARRAY_TEXTURE_LAYERS));

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

    private static final Vector3f LIGHT0_POSITION = new Vector3f();
    private static final Vector3f LIGHT1_POSITION = new Vector3f();
    private static final Vector3f CAMERA_BOB_OFFSET = new Vector3f();

    private static VeilRenderer renderer;
    private static ResourceLocation shaderLocation;
    private static VertexBuffer vbo;

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
    public static void init() {
        Minecraft client = Minecraft.getInstance();
        if (!(client.getResourceManager() instanceof ReloadableResourceManager resourceManager)) {
            throw new IllegalStateException("Client resource manager is " + client.getResourceManager().getClass());
        }

        renderer = new VeilRenderer(resourceManager, client.getWindow());
        VeilImGuiImpl.init(client.getWindow().getWindow());

        BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);
        bufferBuilder.addVertex(-1, 1, 0);
        bufferBuilder.addVertex(-1, -1, 0);
        bufferBuilder.addVertex(1, 1, 0);
        bufferBuilder.addVertex(1, -1, 0);

        vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vbo.bind();
        vbo.upload(bufferBuilder.buildOrThrow());
        VertexBuffer.unbind();
    }

    private static void invalidateTextures(int first, int count) {
        int invalidCount = Math.min(12 - first, count);
        for (int i = first; i < invalidCount; i++) {
            GlStateManager.TEXTURES[i].binding = -1;
        }
    }

    /**
     * Binds the specified texture ids to sequential texture units and invalidates the GLStateManager.
     *
     * @param first    The first unit to bind to
     * @param textures The textures to bind
     */
    public static void bindTextures(int first, IntBuffer textures) {
        invalidateTextures(first, textures.limit());
        glBindTextures(first, textures);
    }

    /**
     * Binds the specified texture ids to sequential texture units and invalidates the GLStateManager.
     *
     * @param first    The first unit to bind to
     * @param textures The textures to bind
     */
    public static void bindTextures(int first, int... textures) {
        invalidateTextures(first, textures.length);
        glBindTextures(first, textures);
    }

    /**
     * Draws a quad onto the full screen using {@link DefaultVertexFormat#POSITION}.
     */
    public static void drawScreenQuad() {
        vbo.bind();
        vbo.draw();
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
        VeilRenderSystem.shaderLocation = shader != null ? shader.getId() : null;
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

        ShaderProgram value = getShader();
        if (value == null) {
            throwShaderError();
        }
        return value;
    }

    /**
     * Clears all pending shader errors and re-queues uniform block ids to shaders.
     */
    @ApiStatus.Internal
    public static void finalizeShaderCompilation() {
        ERRORED_SHADERS.clear();
        UNIFORM_BLOCK_STATE.queueUpload();
    }

    /**
     * Prints an error to console about the current shader.
     * This is useful to debug if a shader has an error while trying to be used.
     */
    public static void throwShaderError() {
        if (VeilRenderSystem.shaderLocation != null && ERRORED_SHADERS.add(VeilRenderSystem.shaderLocation)) {
            Veil.LOGGER.error("Failed to apply shader: {}", VeilRenderSystem.shaderLocation);
        }
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
     * @return Whether {@link GL44C#glBindTextures} is supported
     */
    public static boolean textureMultibindSupported() {
        return VeilRenderSystem.TEXTURE_MULTIBIND_SUPPORTED.getAsBoolean();
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
     * <p>Binds the specified block into the next available binding spot
     * and updates all shaders if the binding index has changed.</p>
     * <p><b>Make sure this is called before trying to use the block on this frame as it may have been overwritten.</b></p>
     *
     * @param block The block to bind
     */
    public static void bind(ShaderBlock<?> block) {
        RenderSystem.assertOnRenderThreadOrInit();
        UNIFORM_BLOCK_STATE.bind(block);
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
     * Binds the specified vertex array and invalidates the vanilla MC immediate buffer state.
     *
     * @param vao The vao to bind
     */
    public static void bindVertexArray(int vao) {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(vao);
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
    public static Vector3fc getLight0Position() {
        return LIGHT0_POSITION;
    }

    /**
     * @return The position of the second light
     */

    public static Vector3fc getLight1Position() {
        return LIGHT1_POSITION;
    }

    /**
     * @return The camera position offset from view bobbing
     */
    public static Vector3fc getCameraBobOffset() {
        return CAMERA_BOB_OFFSET;
    }

    // Internal

    @ApiStatus.Internal
    public static void beginFrame() {
        VeilImGuiImpl.get().beginFrame();
    }

    @ApiStatus.Internal
    public static void endFrame() {
        VeilImGuiImpl.get().endFrame();

        if (Veil.platform().hasErrors()) {
            return;
        }

        RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
        renderer.getFramebufferManager().clear();
        renderer.getDynamicBufferManger().clear();
        glBindFramebuffer(GL_FRAMEBUFFER, 0); // Manual unbind to restore the default mc state

        UNIFORM_BLOCK_STATE.clear();
        VanillaShaderCompiler.clear();
    }

    @ApiStatus.Internal
    public static void shaderUpdate() {
        VeilRenderSystem.shaderLocation = null;
    }

    @ApiStatus.Internal
    public static void resize(int width, int height) {
        if (renderer != null) {
            renderer.getFramebufferManager().resizeFramebuffers(width, height);
            renderer.getDynamicBufferManger().resizeFramebuffers(width, height);
        }
    }

    @ApiStatus.Internal
    public static void close() {
        if (VeilImGuiImpl.get() instanceof NativeResource resource) {
            resource.free();
        }
        VeilOpenCL.tryFree();
        if (renderer != null) {
            renderer.free();
        }
        vbo.close();
    }

    @ApiStatus.Internal
    public static void renderPost() {
        renderer.getPostProcessingManager().runPipeline();
    }

    @ApiStatus.Internal
    public static void setShaderLights(Vector3fc light0, Vector3fc light1) {
        LIGHT0_POSITION.set(light0);
        LIGHT1_POSITION.set(light1);
    }

    @ApiStatus.Internal
    public static void setCameraBobOffset(Vector3fc offset) {
        CAMERA_BOB_OFFSET.set(offset);
    }

    @ApiStatus.Internal
    public static void blit(ProfilerFiller profiler) {
        LightRenderer lightRenderer = renderer.getLightRenderer();
        PostProcessingManager postProcessingManager = renderer.getPostProcessingManager();
        FramebufferManager framebufferManager = renderer.getFramebufferManager();

        profiler.push("lights");
        profiler.push("setup_lights");
        lightRenderer.setup(VeilRenderer.getCullingFrustum());
        profiler.popPush("draw_lights");
        AdvancedFbo lightFbo = framebufferManager.getFramebuffer(VeilFramebuffers.LIGHT);
        if (lightFbo != null) {
            lightFbo.bind(true);
            lightRenderer.render();
        }
        lightRenderer.clear();
        profiler.pop();

        // Applies effects to the final light image
        PostPipeline lightPipeline = postProcessingManager.getPipeline(VeilRenderer.LIGHT_POST);
        if (lightPipeline != null) {
            profiler.push("light_post");
            postProcessingManager.runPipeline(lightPipeline, false);
            profiler.pop();
        }

        PostPipeline compositePipeline = postProcessingManager.getPipeline(VeilRenderer.COMPOSITE);
        if (compositePipeline != null) {
            profiler.push("composite");
            postProcessingManager.runPipeline(compositePipeline, true);
            profiler.pop();
        }

        profiler.pop();
    }
}
