package foundry.veil.impl.client.render.shader.program;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import foundry.veil.Veil;
import foundry.veil.api.client.render.shader.*;
import foundry.veil.api.client.render.shader.program.MutableUniformAccess;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.texture.ShaderTextureSource;
import foundry.veil.api.client.util.VertexFormatCodec;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManger;
import foundry.veil.impl.client.render.shader.DummyResource;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL31C.GL_INVALID_INDEX;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class ShaderProgramImpl implements ShaderProgram {

    public static final VeilShaderSource DUMMY_FRAGMENT_SHADER = new VeilShaderSource(null, "out vec4 fragColor;void main(){fragColor=vec4(1.0);}");

    protected final ResourceLocation id;
    protected final Int2ObjectMap<CompiledShader> shaders;
    protected final Int2ObjectMap<CompiledShader> attachedShaders;
    private final ShaderUniformCache uniforms;
    private final Map<String, ShaderTextureSource> textureSources;
    private final Set<String> definitionDependencies;
    private final ShaderTextureCache textures;
    private final Supplier<Wrapper> wrapper;

    private VertexFormat vertexFormat;
    protected ProgramDefinition definition;
    protected int program;

    public ShaderProgramImpl(ResourceLocation id) {
        this.id = id;
        this.shaders = new Int2ObjectArrayMap<>(2);
        this.attachedShaders = new Int2ObjectArrayMap<>(2);
        this.uniforms = new ShaderUniformCache(this);
        this.textures = new ShaderTextureCache(this);
        this.textureSources = new HashMap<>();
        this.definitionDependencies = new HashSet<>();
        this.wrapper = Suppliers.memoize(() -> {
            Wrapper.constructingProgram = this;
            try {
                return new Wrapper(this);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to wrap shader program: " + this.getId(), e);
            } finally {
                Wrapper.constructingProgram = null;
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
        this.textures.clear();
        this.textureSources.clear();
        this.definitionDependencies.clear();
        this.vertexFormat = null;
    }

    protected void link() throws ShaderException {
        this.uniforms.clear();
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
        return this.uniforms.getUniform(name);
    }

    @Override
    public boolean hasUniform(CharSequence name) {
        return this.program != 0 && this.uniforms.hasUniform(name);
    }

    @Override
    public int getUniformBlock(CharSequence name) {
        if (this.program == 0) {
            return GL_INVALID_INDEX;
        }
        return this.uniforms.getUniformBlock(name);
    }

    @Override
    public boolean hasUniformBlock(CharSequence name) {
        return this.program != 0 && this.uniforms.hasUniformBlock(name);
    }

    @Override
    public int getStorageBlock(CharSequence name) {
        if (this.program == 0) {
            return GL_INVALID_INDEX;
        }
        return this.uniforms.getStorageBlock(name);
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
    public void applyShaderSamplers(@Nullable ShaderTextureSource.Context context, int sampler) {
        if (context != null) {
            this.textureSources.forEach((name, source) -> this.addSampler(name, source.getId(context)));
        }

        this.textures.bind(sampler);
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
        if (this.uniforms.hasSampler(name)) {
            this.textures.put(name, textureId);
        }
    }

    @Override
    public void removeSampler(CharSequence name) {
        this.textures.remove(name);
    }

    @Override
    public void clearSamplers() {
        this.textures.clear();
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

        public static ShaderProgram constructingProgram = null;

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
            this.program.bind();
            this.program.applyShaderSamplers(0);
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
            int sampler = switch (value) {
                case RenderTarget target -> target.getColorTextureId();
                case AbstractTexture texture -> texture.getId();
                case Integer id -> id;
                default -> -1;
            };

            if (sampler != -1) {
                if (sampler == 0) {
                    this.program.removeSampler(name);
                } else {
                    this.program.addSampler(name, sampler);
                }
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

    public static class ShaderWrapper extends Program {

        private final Type type;
        private final ShaderProgram program;

        public ShaderWrapper(Type type, ShaderProgram program) {
            super(type, 0, getName(type, program));
            this.type = type;
            this.program = program;
        }

        private static String getName(Type type, ShaderProgram program) {
            ProgramDefinition definition = program.getDefinition();
            if (definition != null) {
                switch (type) {
                    case VERTEX -> {
                        ProgramDefinition.ShaderSource vertex = definition.vertex();
                        if (vertex != null) {
                            return vertex.location().toString();
                        }
                    }
                    case FRAGMENT -> {
                        ProgramDefinition.ShaderSource fragment = definition.fragment();
                        if (fragment != null) {
                            return fragment.location().toString();
                        }
                    }
                }
            }
            return Veil.MODID + ":dummy_" + type.getName();
        }

        @Override
        public void attachToShader(Shader shader) {
        }

        @Override
        public void close() {
        }

        @Override
        public String getName() {
            return getName(this.type, this.program);
        }

        @Override
        public int getId() {
            Int2ObjectMap<CompiledShader> shaders = this.program.getShaders();
            switch (this.type) {
                case VERTEX -> {
                    CompiledShader vertex = shaders.get(GL_VERTEX_SHADER);
                    return vertex != null ? vertex.id() : 0;
                }
                case FRAGMENT -> {
                    CompiledShader fragment = shaders.get(GL_FRAGMENT_SHADER);
                    return fragment != null ? fragment.id() : 0;
                }
            }
            return super.getId();
        }
    }
}
