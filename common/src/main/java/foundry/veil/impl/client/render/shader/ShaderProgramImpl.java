package foundry.veil.impl.client.render.shader;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.*;
import foundry.veil.api.client.render.shader.program.MutableUniformAccess;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.texture.ShaderTextureSource;
import foundry.veil.api.client.util.VertexFormatCodec;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManger;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL31C.GL_INVALID_INDEX;
import static org.lwjgl.opengl.GL31C.glGetUniformBlockIndex;
import static org.lwjgl.opengl.GL43C.*;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class ShaderProgramImpl implements ShaderProgram {

    public static final VeilShaderSource DUMMY_FRAGMENT_SHADER = new VeilShaderSource(null, "out vec4 fragColor;void main(){fragColor=vec4(1.0);}");

    protected final ResourceLocation id;
    protected final Int2ObjectMap<CompiledShader> shaders;
    protected final Int2ObjectMap<CompiledShader> attachedShaders;
    private final Object2IntMap<CharSequence> uniforms;
    private final Object2IntMap<CharSequence> uniformBlocks;
    private final Object2IntMap<CharSequence> storageBlocks;
    private final Map<String, ShaderTextureSource> textureSources;
    private final Set<String> definitionDependencies;
    private final TextureCache textures;
    private final Supplier<Wrapper> wrapper;

    private VertexFormat vertexFormat;
    protected ProgramDefinition definition;
    protected int program;

    public ShaderProgramImpl(ResourceLocation id) {
        this.id = id;
        this.shaders = new Int2ObjectArrayMap<>(2);
        this.attachedShaders = new Int2ObjectArrayMap<>(2);
        this.uniforms = new Object2IntArrayMap<>();
        this.uniformBlocks = new Object2IntArrayMap<>();
        this.storageBlocks = new Object2IntArrayMap<>();
        this.textures = new TextureCache(this);
        this.textureSources = new HashMap<>();
        this.definitionDependencies = new HashSet<>();
        this.wrapper = Suppliers.memoize(() -> {
            Wrapper.constructing = true;
            try {
                return new Wrapper(this);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to wrap shader program: " + this.getId(), e);
            } finally {
                Wrapper.constructing = false;
            }
        });
    }

    private @Nullable VertexFormat detectVertexFormat() {
        VertexFormat best = null;
        int bestElements = 0;

        int activeAttributes = glGetProgrami(this.program, GL_ACTIVE_ATTRIBUTES);
        Int2ObjectMap<String> names = new Int2ObjectArrayMap<>(activeAttributes);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            for (int i = 0; i < activeAttributes; i++) {
                String name = glGetActiveAttrib(this.program, i, size, type);
                names.put(glGetAttribLocation(this.program, name), name);
            }
        }

        for (VertexFormat format : VertexFormatCodec.getDefaultFormats().values()) {
            List<VertexFormatElement> elements = format.getElements();
            int foundElements = 0;
            if (elements.size() > activeAttributes) {
                continue;
            }

            for (int i = 0; i < elements.size(); i++) {
                if (!format.getElementName(elements.get(i)).equals(names.get(i))) {
                    break;
                }
                foundElements++;
            }

            if (foundElements < elements.size()) {
                continue;
            }

            if (bestElements <= activeAttributes && foundElements > bestElements) {
                best = format;
                bestElements = foundElements;
            }
        }

        return best;
    }

    protected void attachShader(int glType, CompiledShader shader, int activeBuffers) {
        CompiledShader old = this.shaders.put(DynamicBufferManger.getShaderIndex(glType, activeBuffers), shader);
        if (old != null) {
            old.free();
        }
        this.attachedShaders.put(glType, shader);
        glAttachShader(this.program, shader.id());
    }

    protected void detachShaders() {
        if (this.program != 0) {
            for (CompiledShader shader : this.attachedShaders.values()) {
                glDetachShader(this.program, shader.id());
            }
        }
        this.attachedShaders.clear();
    }

    protected void clearShader() {
        this.detachShaders();
        this.uniforms.clear();
        this.uniformBlocks.clear();
        this.textures.clear();
        this.textureSources.clear();
        this.definitionDependencies.clear();
        this.vertexFormat = null;
    }

    protected void link() throws ShaderException {
        this.uniforms.clear();
        this.uniformBlocks.clear();
        this.textures.clear();

        glLinkProgram(this.program);
        if (glGetProgrami(this.program, GL_LINK_STATUS) != GL_TRUE) {
            String log = StringUtils.trim(glGetProgramInfoLog(this.program));
            throw new ShaderException("Failed to link shader", log);
        }

        glValidateProgram(this.program);
        if (glGetProgrami(this.program, GL_VALIDATE_STATUS) != GL_TRUE) {
            String log = StringUtils.trim(glGetProgramInfoLog(this.program));
            Veil.LOGGER.warn("Failed to validate shader ({}) : {}", this.id, log);
        }

        this.attachedShaders.values().forEach(shader -> {
            shader.apply(this);
            this.definitionDependencies.addAll(shader.definitionDependencies());
        });

        this.vertexFormat = this.detectVertexFormat();
    }

    protected void compileInternal(int activeBuffers, ShaderSourceSet sourceSet, ShaderCompiler compiler) throws ShaderException, IOException {
        if (this.program == 0) {
            this.program = glCreateProgram();
        }

        try {
            Int2ObjectMap<ProgramDefinition.ShaderSource> shaders = this.definition.shaders();
            for (Int2ObjectMap.Entry<ProgramDefinition.ShaderSource> entry : shaders.int2ObjectEntrySet()) {
                int glType = entry.getIntKey();
                ProgramDefinition.ShaderSource source = entry.getValue();
                this.attachShader(glType, compiler.compile(glType, source.sourceType(), sourceSet.getTypeConverter(glType).idToFile(source.location())), activeBuffers);
            }

            // Fragment shaders aren't strictly necessary if the fragment output isn't used,
            // however mac shaders don't work without a fragment shader. This adds a "dummy" fragment shader
            // on mac specifically for all rendering shaders.
            if (Minecraft.ON_OSX && !shaders.containsKey(GL_COMPUTE_SHADER) && !shaders.containsKey(GL_FRAGMENT_SHADER)) {
                this.attachShader(GL_FRAGMENT_SHADER, compiler.compile(GL_FRAGMENT_SHADER, ProgramDefinition.SourceType.GLSL, DUMMY_FRAGMENT_SHADER), activeBuffers);
            }

            this.link();
        } catch (Exception e) {
            this.freeInternal(); // F
            throw e;
        }
    }

    public void compile(int activeBuffers, ShaderSourceSet sourceSet, @Nullable ProgramDefinition definition, ShaderCompiler compiler) throws ShaderException, IOException {
        this.definition = definition;
        this.clearShader();
        if (this.definition != null) {
            this.textureSources.putAll(this.definition.textures());
        }
        for (CompiledShader shader : this.shaders.values()) {
            shader.free();
        }
        this.shaders.clear();
        this.compileInternal(activeBuffers, sourceSet, compiler);
    }

    public boolean setActiveBuffers(int activeBuffers) throws ShaderException {
        for (int shaderType : this.attachedShaders.keySet()) {
            if (!this.shaders.containsKey(DynamicBufferManger.getShaderIndex(shaderType, activeBuffers))) {
                return true;
            }
        }

        int[] shaders = this.attachedShaders.keySet().toIntArray();
        this.detachShaders();
        for (int shaderType : shaders) {
            CompiledShader shader = this.shaders.get(DynamicBufferManger.getShaderIndex(shaderType, activeBuffers));
            glAttachShader(this.program, shader.id());
            this.attachedShaders.put(shaderType, shader);
        }

        this.link();
        return false;
    }

    public void updateActiveBuffers(int activeBuffers, ShaderSourceSet sourceSet, ShaderCompiler compiler) throws ShaderException, IOException {
        this.detachShaders();
        this.compileInternal(activeBuffers, sourceSet, compiler);
    }

    protected void freeInternal() {
        this.clearShader();
        for (CompiledShader shader : this.shaders.values()) {
            shader.free();
        }
        this.shaders.clear();
        if (this.program > 0) {
            glDeleteProgram(this.program);
            this.program = 0;
        }
    }

    @Override
    public void free() {
        this.freeInternal();
    }

    @Override
    public Int2ObjectMap<CompiledShader> getShaders() {
        return this.attachedShaders;
    }

    @Override
    public @Nullable VertexFormat getFormat() {
        return this.vertexFormat;
    }

    @Override
    public Set<String> getDefinitionDependencies() {
        return this.definitionDependencies;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public Wrapper toShaderInstance() {
        return this.wrapper.get();
    }

    @Override
    public int getUniform(CharSequence name) {
        if (this.program == 0) {
            return -1;
        }
        return this.uniforms.computeIfAbsent(name, (ToIntFunction<? super CharSequence>) k -> glGetUniformLocation(this.program, k));
    }

    @Override
    public int getUniformBlock(CharSequence name) {
        if (this.program == 0) {
            return GL_INVALID_INDEX;
        }
        return this.uniformBlocks.computeIfAbsent(name, k -> glGetUniformBlockIndex(this.program, name));
    }

    @Override
    public int getStorageBlock(CharSequence name) {
        if (this.program == 0) {
            return GL_INVALID_INDEX;
        }
        return this.storageBlocks.computeIfAbsent(name, k -> glGetProgramResourceIndex(this.program, GL_SHADER_STORAGE_BLOCK, name));
    }

    @Override
    public int getProgram() {
        return this.program;
    }

    @Override
    public @Nullable ProgramDefinition getDefinition() {
        return this.definition;
    }

    @Override
    public int applyShaderSamplers(@Nullable ShaderTextureSource.Context context, int sampler) {
        if (context != null) {
            this.textureSources.forEach((name, source) -> this.addSampler(name, source.getId(context)));
        }

        return this.textures.bind(sampler);
    }

    @Override
    public void addSamplerListener(SamplerListener listener) {
        this.textures.addSamplerListener(listener);
    }

    @Override
    public void removeSamplerListener(SamplerListener listener) {
        this.textures.removeSamplerListener(listener);
    }

    @Override
    public void addSampler(CharSequence name, int textureId) {
        this.textures.put(name, textureId);
    }

    @Override
    public void removeSampler(CharSequence name) {
        this.textures.remove(name);
    }

    @Override
    public void clearSamplers() {
        this.textures.clear();
    }

    private static class TextureCache {

        private final ShaderProgram program;
        private final Object2IntMap<CharSequence> textures;
        private final Object2IntMap<CharSequence> boundSamplers;
        private final ObjectSet<SamplerListener> listeners;
        private boolean dirty;
        private IntBuffer bindings;

        private TextureCache(ShaderProgram program) {
            this.program = program;
            this.textures = new Object2IntArrayMap<>();
            this.textures.defaultReturnValue(-1);
            this.boundSamplers = new Object2IntArrayMap<>();
            this.listeners = new ObjectArraySet<>();
            this.bindings = null;
        }

        private int uploadTextures(int start, BiConsumer<Integer, Integer> textureConsumer) {
            this.boundSamplers.clear();
            if (this.textures.isEmpty()) {
                return start;
            }

            int maxSampler = VeilRenderSystem.maxCombinedTextureUnits();
            int count = 1;
            textureConsumer.accept(start, MissingTextureAtlasSprite.getTexture().getId());

            for (Object2IntMap.Entry<CharSequence> entry : this.textures.object2IntEntrySet()) {
                CharSequence name = entry.getKey();
                if (this.program.getUniform(name) == -1) {
                    continue;
                }

                // If there are too many samplers, then refer back to the missing texture
                int sampler = start + count;
                if (sampler >= maxSampler) {
                    this.program.setInt(name, 0);
                    Veil.LOGGER.error("Too many samplers were bound for shader (max {}): {}", maxSampler, this.program.getId());
                    continue;
                }

                // If the texture is "missing", then refer back to the bound missing texture
                int textureId = entry.getIntValue();
                if (textureId == 0) {
                    this.program.setInt(name, 0);
                    continue;
                }

                textureConsumer.accept(sampler, textureId);
                this.program.setInt(name, sampler);
                this.boundSamplers.put(name, sampler);
                count++;
            }
            for (SamplerListener listener : this.listeners) {
                listener.onUpdateSamplers(this.boundSamplers);
            }
            return start + count;
        }

        public int bind(int start) {
            if (VeilRenderSystem.textureMultibindSupported()) {
                if (this.dirty) {
                    this.dirty = false;

                    // Not enough space, so realloc
                    if (this.bindings == null || this.bindings.capacity() < 1 + this.textures.size()) {
                        this.bindings = MemoryUtil.memRealloc(this.bindings, 1 + this.textures.size());
                    }

                    this.bindings.clear();
                    int end = this.uploadTextures(start, (sampler, id) -> this.bindings.put(id));
                    if (end == start) {
                        this.bindings.position(0);
                        return start;
                    }

                    this.bindings.flip();
                }
                if (this.bindings != null && this.bindings.limit() > 0) {
                    VeilRenderSystem.bindTextures(start, this.bindings);
                    return start + this.bindings.limit();
                }
                return start;
            }

            // Ignored for normal binding
            this.dirty = false;

            int activeTexture = GlStateManager._getActiveTexture();
            int end = this.uploadTextures(start, (sampler, id) -> {
                RenderSystem.activeTexture(GL_TEXTURE0 + sampler);
                if (sampler >= 12) {
                    glBindTexture(GL_TEXTURE_2D, id);
                } else {
                    RenderSystem.bindTexture(id);
                }
            });
            RenderSystem.activeTexture(activeTexture);
            return end;
        }

        public void addSamplerListener(SamplerListener listener) {
            this.listeners.add(listener);
        }

        public void removeSamplerListener(SamplerListener listener) {
            this.listeners.remove(listener);
        }

        public void put(CharSequence name, int textureId) {
            this.dirty |= this.textures.put(name, textureId) != textureId;
        }

        public void remove(CharSequence name) {
            if (this.textures.removeInt(name) != 0) {
                this.dirty = true;
            }
        }

        public void clear() {
            this.textures.clear();
            this.boundSamplers.clear();
            if (this.bindings != null) {
                MemoryUtil.memFree(this.bindings);
                this.bindings = null;
            }
            this.dirty = true;
        }
    }

    /**
     * @author Ocelot
     */
    public static class Wrapper extends ShaderInstance {

        private static final byte[] DUMMY_SHADER = """
                {
                    "vertex": "dummy",
                    "fragment": "dummy"
                }
                """.getBytes(StandardCharsets.UTF_8);
        private static final Resource RESOURCE = new DummyResource(() -> new ByteArrayInputStream(DUMMY_SHADER));
        private static final VertexFormat DUMMY_FORMAT = VertexFormat.builder().build();

        public static boolean constructing = false;

        private final ShaderProgram program;

        private Wrapper(ShaderProgram program) throws IOException {
            super(name -> Optional.of(RESOURCE), "", DUMMY_FORMAT);
            this.program = program;
        }

        @Override
        public void close() {
        }

        @Override
        public void clear() {
            ShaderProgram.unbind();
        }

        @Override
        public void apply() {
            this.program.setup();
        }

        @Override
        public void attachToProgram() {
            throw new UnsupportedOperationException("Cannot attach shader program wrapper");
        }

        @Override
        public void markDirty() {
        }

        @Override
        public @Nullable UniformWrapper getUniform(String name) {
            if (this.program != null && this.program.getUniform(name) == -1) {
                return null;
            }
            return (UniformWrapper) this.uniformMap.computeIfAbsent(name, unused -> new UniformWrapper(() -> this.program, name));
        }

        @Override
        public void setSampler(String name, Object value) {
            int sampler = -1;
            switch (value) {
                case RenderTarget target -> sampler = target.getColorTextureId();
                case AbstractTexture texture -> sampler = texture.getId();
                case Integer id -> sampler = id;
                default -> {
                }
            }

            if (sampler != -1) {
                this.program.addSampler(name, sampler);
            }
        }

        @Override
        public VertexFormat getVertexFormat() {
            VertexFormat format = this.program.getFormat();
            return format != null ? format : super.getVertexFormat();
        }

        /**
         * @return The backing shader program
         */
        public ShaderProgram program() {
            return this.program;
        }
    }

    /**
     * @author Ocelot
     */
    public static class UniformWrapper extends Uniform {

        private static final Matrix2f MAT2X2 = new Matrix2f();
        private static final Matrix3f MAT3X3 = new Matrix3f();
        private static final Matrix3x2f MAT3X2 = new Matrix3x2f();
        private static final Matrix4x3f MAT4X3 = new Matrix4x3f();
        private static final Matrix4f MAT4X4 = new Matrix4f();

        private final Supplier<MutableUniformAccess> access;

        public UniformWrapper(Supplier<MutableUniformAccess> access, String name) {
            super(name, UT_INT1, 0, null);
            super.close(); // Free constructor allocated resources
            this.access = access;
        }

        @Override
        public void setLocation(int location) {
        }

        @Override
        public void set(int index, float value) {
            throw new UnsupportedOperationException("Use absolute set");
        }

        @Override
        public void set(float value) {
            this.access.get().setFloat(this.getName(), value);
        }

        @Override
        public void set(float x, float y) {
            this.access.get().setVector(this.getName(), x, y);
        }

        @Override
        public void set(float x, float y, float z) {
            this.access.get().setVector(this.getName(), x, y, z);
        }

        @Override
        public void set(float x, float y, float z, float w) {
            this.access.get().setVector(this.getName(), x, y, z, w);
        }

        @Override
        public void set(@NotNull Vector3f value) {
            this.access.get().setVector(this.getName(), value);
        }

        @Override
        public void set(@NotNull Vector4f value) {
            this.access.get().setVector(this.getName(), value);
        }

        @Override
        public void setSafe(float x, float y, float z, float w) {
            this.set(x, y, z, w);
        }

        @Override
        public void set(int value) {
            this.access.get().setInt(this.getName(), value);
        }

        @Override
        public void set(int x, int y) {
            this.access.get().setVector(this.getName(), x, y);
        }

        @Override
        public void set(int x, int y, int z) {
            this.access.get().setVector(this.getName(), x, y, z);
        }

        @Override
        public void set(int x, int y, int z, int w) {
            this.access.get().setVector(this.getName(), x, y, z, w);
        }

        @Override
        public void setSafe(int x, int y, int z, int w) {
            this.set(x, y, z, w);
        }

        @Override
        public void set(float[] values) {
            switch (values.length) {
                case 1 -> this.set(values[0]);
                case 2 -> this.set(values[0], values[1]);
                case 3 -> this.set(values[0], values[1], values[2]);
                case 4 -> this.set(values[0], values[1], values[2], values[3]);
                default -> throw new UnsupportedOperationException("Invalid value array: " + Arrays.toString(values));
            }
        }

        @Override
        public void setMat2x2(float m00, float m01, float m10, float m11) {
            this.access.get().setMatrix(this.getName(), MAT2X2.set(m00, m01, m10, m11));
        }

        @Override
        public void setMat2x3(float m00, float m01, float m02, float m10, float m11, float m12) {
            this.access.get().setMatrix(this.getName(), MAT3X2.set(
                    m00, m10,
                    m01, m11,
                    m02, m12
            ), true);
        }

        @Override
        public void setMat2x4(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMat3x2(float m00, float m01, float m10, float m11, float m20, float m21) {
            this.access.get().setMatrix(this.getName(), MAT3X2.set(m00, m01, m10, m11, m20, m21));
        }

        @Override
        public void setMat3x3(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
            this.access.get().setMatrix(this.getName(), MAT3X3.set(m00, m01, m02, m10, m11, m12, m20, m21, m22));
        }

        @Override
        public void setMat3x4(
                float m00,
                float m01,
                float m02,
                float m03,
                float m10,
                float m11,
                float m12,
                float m13,
                float m20,
                float m21,
                float m22,
                float m23
        ) {
            this.access.get().setMatrix(this.getName(), MAT4X3.set(
                    m00, m10, m20,
                    m01, m11, m21,
                    m02, m12, m22,
                    m03, m13, m23
            ), true);
        }

        @Override
        public void setMat4x2(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMat4x3(
                float m00,
                float m01,
                float m02,
                float m03,
                float m10,
                float m11,
                float m12,
                float m13,
                float m20,
                float m21,
                float m22,
                float m23) {
            this.access.get().setMatrix(this.getName(), MAT4X3.set(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23));
        }

        @Override
        public void setMat4x4(
                float m00,
                float m01,
                float m02,
                float m03,
                float m10,
                float m11,
                float m12,
                float m13,
                float m20,
                float m21,
                float m22,
                float m23,
                float m30,
                float m31,
                float m32,
                float m33
        ) {
            this.access.get().setMatrix(this.getName(), MAT4X4.set(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33));
        }

        @Override
        public void set(@NotNull Matrix3f value) {
            this.access.get().setMatrix(this.getName(), value);
        }

        @Override
        public void set(@NotNull Matrix4f value) {
            this.access.get().setMatrix(this.getName(), value);
        }

        @Override
        public void upload() {
        }

        @Override
        public void close() {
        }

        @Override
        public int getLocation() {
            return this.access.get().getUniform(this.getName());
        }
    }
}
