package foundry.veil.impl.client.render.shader.program;

import foundry.veil.api.client.render.shader.program.ShaderProgram;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL31C.*;
import static org.lwjgl.opengl.GL32C.*;
import static org.lwjgl.opengl.GL40C.*;
import static org.lwjgl.opengl.GL42C.*;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BLOCK;
import static org.lwjgl.opengl.GL43C.glGetProgramResourceIndex;

@ApiStatus.Internal
public class ShaderUniformCache {

    private static final IntSet SAMPLERS = IntSet.of(
            GL_SAMPLER_1D,
            GL_SAMPLER_2D,
            GL_SAMPLER_3D,
            GL_SAMPLER_CUBE,
            GL_SAMPLER_1D_SHADOW,
            GL_SAMPLER_2D_SHADOW,
            GL_SAMPLER_1D_ARRAY,
            GL_SAMPLER_2D_ARRAY,
            GL_SAMPLER_1D_ARRAY_SHADOW,
            GL_SAMPLER_2D_ARRAY_SHADOW,
            GL_SAMPLER_2D_MULTISAMPLE,
            GL_SAMPLER_2D_MULTISAMPLE_ARRAY,
            GL_SAMPLER_CUBE_SHADOW,
            GL_SAMPLER_BUFFER,
            GL_SAMPLER_2D_RECT,
            GL_SAMPLER_2D_RECT_SHADOW,
            GL_INT_SAMPLER_1D,
            GL_INT_SAMPLER_2D,
            GL_INT_SAMPLER_3D,
            GL_INT_SAMPLER_CUBE,
            GL_INT_SAMPLER_1D_ARRAY,
            GL_INT_SAMPLER_2D_ARRAY,
            GL_INT_SAMPLER_2D_MULTISAMPLE,
            GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY,
            GL_INT_SAMPLER_BUFFER,
            GL_INT_SAMPLER_2D_RECT,
            GL_UNSIGNED_INT_SAMPLER_1D,
            GL_UNSIGNED_INT_SAMPLER_2D,
            GL_UNSIGNED_INT_SAMPLER_3D,
            GL_UNSIGNED_INT_SAMPLER_CUBE,
            GL_UNSIGNED_INT_SAMPLER_1D_ARRAY,
            GL_UNSIGNED_INT_SAMPLER_2D_ARRAY,
            GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE,
            GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY,
            GL_UNSIGNED_INT_SAMPLER_BUFFER,
            GL_UNSIGNED_INT_SAMPLER_2D_RECT);

    private static final Int2ObjectMap<String> NAMES;

    static {
        Int2ObjectMap<String> names = new Int2ObjectArrayMap<>(105);
        names.put(GL_FLOAT, "float");
        names.put(GL_FLOAT_VEC2, "vec2");
        names.put(GL_FLOAT_VEC3, "vec3");
        names.put(GL_FLOAT_VEC4, "vec4");
        names.put(GL_DOUBLE, "double");
        names.put(GL_DOUBLE_VEC2, "dvec2");
        names.put(GL_DOUBLE_VEC3, "dvec3");
        names.put(GL_DOUBLE_VEC4, "dvec4");
        names.put(GL_INT, "int");
        names.put(GL_INT_VEC2, "ivec2");
        names.put(GL_INT_VEC3, "ivec3");
        names.put(GL_INT_VEC4, "ivec4");
        names.put(GL_UNSIGNED_INT, "unsigned int");
        names.put(GL_UNSIGNED_INT_VEC2, "uvec2");
        names.put(GL_UNSIGNED_INT_VEC3, "uvec3");
        names.put(GL_UNSIGNED_INT_VEC4, "uvec4");
        names.put(GL_BOOL, "bool");
        names.put(GL_BOOL_VEC2, "bvec2");
        names.put(GL_BOOL_VEC3, "bvec3");
        names.put(GL_BOOL_VEC4, "bvec4");
        names.put(GL_FLOAT_MAT2, "mat2");
        names.put(GL_FLOAT_MAT3, "mat3");
        names.put(GL_FLOAT_MAT4, "mat4");
        names.put(GL_FLOAT_MAT2x3, "mat2x3");
        names.put(GL_FLOAT_MAT2x4, "mat2x4");
        names.put(GL_FLOAT_MAT3x2, "mat3x2");
        names.put(GL_FLOAT_MAT3x4, "mat3x4");
        names.put(GL_FLOAT_MAT4x2, "mat4x2");
        names.put(GL_FLOAT_MAT4x3, "mat4x3");
        names.put(GL_DOUBLE_MAT2, "dmat2");
        names.put(GL_DOUBLE_MAT3, "dmat3");
        names.put(GL_DOUBLE_MAT4, "dmat4");
        names.put(GL_DOUBLE_MAT2x3, "dmat2x3");
        names.put(GL_DOUBLE_MAT2x4, "dmat2x4");
        names.put(GL_DOUBLE_MAT3x2, "dmat3x2");
        names.put(GL_DOUBLE_MAT3x4, "dmat3x4");
        names.put(GL_DOUBLE_MAT4x2, "dmat4x2");
        names.put(GL_DOUBLE_MAT4x3, "dmat4x3");
        names.put(GL_SAMPLER_1D, "sampler1D");
        names.put(GL_SAMPLER_2D, "sampler2D");
        names.put(GL_SAMPLER_3D, "sampler3D");
        names.put(GL_SAMPLER_CUBE, "samplerCube");
        names.put(GL_SAMPLER_1D_SHADOW, "sampler1DShadow");
        names.put(GL_SAMPLER_2D_SHADOW, "sampler2DShadow");
        names.put(GL_SAMPLER_1D_ARRAY, "sampler1DArray");
        names.put(GL_SAMPLER_2D_ARRAY, "sampler2DArray");
        names.put(GL_SAMPLER_1D_ARRAY_SHADOW, "sampler1DArrayShadow");
        names.put(GL_SAMPLER_2D_ARRAY_SHADOW, "sampler2DArrayShadow");
        names.put(GL_SAMPLER_2D_MULTISAMPLE, "sampler2DMS");
        names.put(GL_SAMPLER_2D_MULTISAMPLE_ARRAY, "sampler2DMSArray");
        names.put(GL_SAMPLER_CUBE_SHADOW, "samplerCubeShadow");
        names.put(GL_SAMPLER_BUFFER, "samplerBuffer");
        names.put(GL_SAMPLER_2D_RECT, "sampler2DRect");
        names.put(GL_SAMPLER_2D_RECT_SHADOW, "sampler2DRectShadow");
        names.put(GL_INT_SAMPLER_1D, "isampler1D");
        names.put(GL_INT_SAMPLER_2D, "isampler2D");
        names.put(GL_INT_SAMPLER_3D, "isampler3D");
        names.put(GL_INT_SAMPLER_CUBE, "isamplerCube");
        names.put(GL_INT_SAMPLER_1D_ARRAY, "isampler1DArray");
        names.put(GL_INT_SAMPLER_2D_ARRAY, "isampler2DArray");
        names.put(GL_INT_SAMPLER_2D_MULTISAMPLE, "isampler2DMS");
        names.put(GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY, "isampler2DMSArray");
        names.put(GL_INT_SAMPLER_BUFFER, "isamplerBuffer");
        names.put(GL_INT_SAMPLER_2D_RECT, "isampler2DRect");
        names.put(GL_UNSIGNED_INT_SAMPLER_1D, "usampler1D");
        names.put(GL_UNSIGNED_INT_SAMPLER_2D, "usampler2D");
        names.put(GL_UNSIGNED_INT_SAMPLER_3D, "usampler3D");
        names.put(GL_UNSIGNED_INT_SAMPLER_CUBE, "usamplerCube");
        names.put(GL_UNSIGNED_INT_SAMPLER_1D_ARRAY, "usampler2DArray");
        names.put(GL_UNSIGNED_INT_SAMPLER_2D_ARRAY, "usampler2DArray");
        names.put(GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE, "usampler2DMS");
        names.put(GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY, "usampler2DMSArray");
        names.put(GL_UNSIGNED_INT_SAMPLER_BUFFER, "usamplerBuffer");
        names.put(GL_UNSIGNED_INT_SAMPLER_2D_RECT, "usampler2DRect");
        names.put(GL_IMAGE_1D, "image1D");
        names.put(GL_IMAGE_2D, "image2D");
        names.put(GL_IMAGE_3D, "image3D");
        names.put(GL_IMAGE_2D_RECT, "image2DRect");
        names.put(GL_IMAGE_CUBE, "imageCube");
        names.put(GL_IMAGE_BUFFER, "imageBuffer");
        names.put(GL_IMAGE_1D_ARRAY, "image1DArray");
        names.put(GL_IMAGE_2D_ARRAY, "image2DArray");
        names.put(GL_IMAGE_2D_MULTISAMPLE, "image2DMS");
        names.put(GL_IMAGE_2D_MULTISAMPLE_ARRAY, "image2DMSArray");
        names.put(GL_INT_IMAGE_1D, "iimage1D");
        names.put(GL_INT_IMAGE_2D, "iimage2D");
        names.put(GL_INT_IMAGE_3D, "iimage3D");
        names.put(GL_INT_IMAGE_2D_RECT, "iimage2DRect");
        names.put(GL_INT_IMAGE_CUBE, "iimageCube");
        names.put(GL_INT_IMAGE_BUFFER, "iimageBuffer");
        names.put(GL_INT_IMAGE_1D_ARRAY, "iimage1DArray");
        names.put(GL_INT_IMAGE_2D_ARRAY, "iimage2DArray");
        names.put(GL_INT_IMAGE_2D_MULTISAMPLE, "iimage2DMS");
        names.put(GL_INT_IMAGE_2D_MULTISAMPLE_ARRAY, "iimage2DMSArray");
        names.put(GL_UNSIGNED_INT_IMAGE_1D, "uimage1D");
        names.put(GL_UNSIGNED_INT_IMAGE_2D, "uimage2D");
        names.put(GL_UNSIGNED_INT_IMAGE_3D, "uimage3D");
        names.put(GL_UNSIGNED_INT_IMAGE_2D_RECT, "uimage2DRect");
        names.put(GL_UNSIGNED_INT_IMAGE_CUBE, "uimageCube");
        names.put(GL_UNSIGNED_INT_IMAGE_BUFFER, "uimageBuffer");
        names.put(GL_UNSIGNED_INT_IMAGE_1D_ARRAY, "uimage1DArray");
        names.put(GL_UNSIGNED_INT_IMAGE_2D_ARRAY, "uimage2DArray");
        names.put(GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE, "uimage2DMS");
        names.put(GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE_ARRAY, "uimage2DMSArray");
        names.put(GL_UNSIGNED_INT_ATOMIC_COUNTER, "atomic_uint");
        NAMES = Int2ObjectMaps.unmodifiable(names);
    }

    private final ShaderProgram shader;
    private final ObjectSet<CharSequence> samplers;
    private final Object2IntMap<CharSequence> uniforms;
    private final Object2IntMap<CharSequence> uniformBlocks;
    private final Object2IntMap<CharSequence> storageBlocks;
    private boolean requested;

    public ShaderUniformCache(ShaderProgram shader) {
        this.shader = shader;
        this.samplers = new ObjectArraySet<>();
        this.uniforms = new Object2IntOpenHashMap<>();
        this.uniformBlocks = new Object2IntArrayMap<>();
        this.storageBlocks = new Object2IntArrayMap<>();
        this.requested = false;
    }

    public void clear() {
        this.samplers.clear();
        this.uniforms.clear();
        this.uniformBlocks.clear();
        this.storageBlocks.clear();
        this.requested = false;
    }

    private void updateUniforms() {
        this.requested = true;

        int program = this.shader.getProgram();
        int uniformCount = glGetProgrami(program, GL_ACTIVE_UNIFORMS);
        int maxUniformLength = glGetProgrami(program, GL_ACTIVE_UNIFORM_MAX_LENGTH);
        int uniformBlockCount = glGetProgrami(program, GL_ACTIVE_UNIFORM_BLOCKS);
        int maxUniformBlockLength = glGetProgrami(program, GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            for (int i = 0; i < uniformCount; i++) {
                String name = glGetActiveUniform(program, i, maxUniformLength, size, type);
                int location = glGetUniformLocation(program, name);
                this.uniforms.put(name, location);
                if (isSampler(type.get(0))) {
                    this.samplers.add(name);
                }
            }

            for (int i = 0; i < uniformBlockCount; i++) {
                String name = glGetActiveUniformBlockName(program, i, maxUniformBlockLength);
                int location = glGetUniformBlockIndex(program, name);
                this.uniformBlocks.put(name, location);
            }
        }
    }

    public int getUniform(CharSequence name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.uniforms.getOrDefault(name, -1);
    }

    public boolean hasUniform(CharSequence name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.uniforms.containsKey(name);
    }

    public int getUniformBlock(CharSequence name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.uniformBlocks.getOrDefault(name, GL_INVALID_INDEX);
    }

    public boolean hasUniformBlock(CharSequence name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.uniformBlocks.containsKey(name);
    }

    public int getStorageBlock(CharSequence name) {
        return this.storageBlocks.computeIfAbsent(name, k -> glGetProgramResourceIndex(this.shader.getProgram(), GL_SHADER_STORAGE_BLOCK, name));
    }

    public boolean hasSampler(CharSequence name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.samplers.contains(name);
    }

    public static boolean isSampler(int type) {
        return SAMPLERS.contains(type);
    }

    public static String getName(int type) {
        return NAMES.getOrDefault(type, "0x%04X".formatted(type));
    }
}
