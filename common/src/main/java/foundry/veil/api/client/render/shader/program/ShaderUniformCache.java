package foundry.veil.api.client.render.shader.program;

import foundry.veil.api.client.render.VeilRenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import static org.lwjgl.opengl.ARBProgramInterfaceQuery.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL31C.*;
import static org.lwjgl.opengl.GL32C.*;
import static org.lwjgl.opengl.GL42C.*;

/**
 * Queries shader programs for all relevant uniform data.
 *
 * @author Ocelot
 * @since 1.0.0
 */
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

    private final IntSupplier shader;
    private final Object2ObjectMap<String, Uniform> samplers;
    private final Object2ObjectMap<String, Uniform> uniforms;
    private final Object2ObjectMap<String, UniformBlock> uniformBlocks;
    private final Object2ObjectMap<String, StorageBlock> storageBlocks;
    private final Object2ObjectMap<String, Uniform> samplersView;
    private final Object2ObjectMap<String, Uniform> uniformsView;
    private final Object2ObjectMap<String, UniformBlock> uniformBlocksView;
    private final Object2ObjectMap<String, StorageBlock> storageBlocksView;
    private boolean requested;

    public ShaderUniformCache(IntSupplier shader) {
        this.shader = shader;
        this.samplers = new Object2ObjectOpenHashMap<>();
        this.uniforms = new Object2ObjectOpenHashMap<>();
        this.uniformBlocks = new Object2ObjectOpenHashMap<>();
        this.storageBlocks = new Object2ObjectOpenHashMap<>();
        this.samplersView = Object2ObjectMaps.unmodifiable(this.samplers);
        this.uniformsView = Object2ObjectMaps.unmodifiable(this.uniforms);
        this.uniformBlocksView = Object2ObjectMaps.unmodifiable(this.uniformBlocks);
        this.storageBlocksView = Object2ObjectMaps.unmodifiable(this.storageBlocks);
        this.requested = false;
    }

    /**
     * Clears the cache, rebuilding it the next time {@link #getUniform(CharSequence)} is called.
     */
    public void clear() {
        this.samplers.clear();
        this.uniforms.clear();
        this.uniformBlocks.clear();
        this.storageBlocks.clear();
        this.requested = false;
    }

    private void updateUniforms() {
        this.requested = true;

        int program = this.shader.getAsInt();
        if (VeilRenderSystem.programInterfaceQuerySupported()) {
            int uniformCount = glGetProgramInterfacei(program, GL_UNIFORM, GL_ACTIVE_RESOURCES);
            int uniformBlockCount = glGetProgramInterfacei(program, GL_UNIFORM_BLOCK, GL_ACTIVE_RESOURCES);
            int storageBlockCount = glGetProgramInterfacei(program, GL_SHADER_STORAGE_BLOCK, GL_ACTIVE_RESOURCES);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer uniformProperties = stack.ints(GL_BLOCK_INDEX, GL_ARRAY_SIZE, GL_NAME_LENGTH, GL_LOCATION, GL_TYPE);
                IntBuffer uniformBlockProperties = stack.ints(GL_NAME_LENGTH, GL_BUFFER_DATA_SIZE, GL_NUM_ACTIVE_VARIABLES);
                IntBuffer uniformBlockFieldProperties = stack.ints(GL_NAME_LENGTH, GL_ARRAY_SIZE, GL_ARRAY_STRIDE, GL_OFFSET, GL_TYPE, GL_ATOMIC_COUNTER_BUFFER_INDEX);
                IntBuffer uniformBufferFieldProperties = stack.ints(GL_NAME_LENGTH, GL_ARRAY_SIZE, GL_ARRAY_STRIDE, GL_OFFSET, GL_TYPE);
                IntBuffer activeVariables = stack.ints(GL_ACTIVE_VARIABLES);
                IntBuffer values = stack.mallocInt(6);

                for (int i = 0; i < uniformCount; i++) {
                    glGetProgramResourceiv(program, GL_UNIFORM, i, uniformProperties, null, values);

                    // Don't handle uniform blocks
                    int blockIndex = values.get(0);
                    if (blockIndex != GL_INVALID_INDEX) {
                        continue;
                    }

                    int length = values.get(1);
                    int type = values.get(4);
                    String resourceName = glGetProgramResourceName(program, GL_UNIFORM, i, values.get(2));
                    String baseName = resourceName.contains("[") ? resourceName.substring(0, resourceName.indexOf('[')) : resourceName;

                    for (int j = 0; j < length; j++) {
                        String name = length > 1 ? baseName + '[' + j + ']' : baseName;
                        int location = values.get(3) + j;
                        Uniform uniform = new Uniform(name, location, 0, type, 1);
                        this.uniforms.put(name, uniform);
                        if (isSampler(type)) {
                            this.samplers.put(name, uniform);
                        }
                    }

                    if (length > 1) {
                        Uniform uniform = new Uniform(baseName, values.get(3), 0, type, length);
                        this.uniforms.put(baseName, uniform);
                        if (isSampler(type)) {
                            this.samplers.put(baseName, uniform);
                        }
                    }
                }

                for (int i = 0; i < uniformBlockCount; i++) {
                    glGetProgramResourceiv(program, GL_UNIFORM_BLOCK, i, uniformBlockProperties, null, values);
                    String blockName = glGetProgramResourceName(program, GL_UNIFORM_BLOCK, i, values.get(0));

                    int size = values.get(1);
                    int fieldCount = values.get(2);
                    List<Uniform> fields = new ArrayList<>(fieldCount);

                    IntBuffer fieldIndices = MemoryUtil.memAllocInt(fieldCount);
                    glGetProgramResourceiv(program, GL_UNIFORM_BLOCK, i, activeVariables, null, fieldIndices);
                    try {
                        for (int j = 0; j < fieldCount; j++) {
                            glGetProgramResourceiv(program, GL_UNIFORM, fieldIndices.get(j), uniformBlockFieldProperties, null, values);

                            // It must be an atomic counter, so ignore it
                            if (values.get(5) != -1) {
                                continue;
                            }

                            String name = glGetProgramResourceName(program, GL_UNIFORM, fieldIndices.get(j), values.get(0));
                            int arrayLength = values.get(1);
                            int offset = values.get(3);
                            int type = values.get(4);
                            if (arrayLength > 0) {
                                int stride = values.get(2);
                                String nameBase = name.substring(0, name.length() - 3);
                                for (int k = 0; k < arrayLength; k++) {
                                    fields.add(new Uniform(nameBase + '[' + k + ']', -1, offset + stride * k, type, 1));
                                }
                            } else {
                                fields.add(new Uniform(name, -1, offset, type, 1));
                            }
                        }
                        this.uniformBlocks.put(blockName, new UniformBlock(blockName, i, size, fields.toArray(Uniform[]::new)));
                    } finally {
                        MemoryUtil.memFree(fieldIndices);
                    }
                }

                for (int i = 0; i < storageBlockCount; i++) {
                    glGetProgramResourceiv(program, GL_SHADER_STORAGE_BLOCK, i, uniformBlockProperties, null, values);
                    String blockName = glGetProgramResourceName(program, GL_SHADER_STORAGE_BLOCK, i, values.get(0));

                    int size = values.get(1);
                    int fieldCount = values.get(2);
                    List<Uniform> fields = new ArrayList<>(fieldCount);

                    IntBuffer fieldIndices = MemoryUtil.memAllocInt(fieldCount);
                    glGetProgramResourceiv(program, GL_SHADER_STORAGE_BLOCK, i, activeVariables, null, fieldIndices);
                    try {
                        for (int j = 0; j < fieldCount; j++) {
                            glGetProgramResourceiv(program, GL_BUFFER_VARIABLE, fieldIndices.get(j), uniformBufferFieldProperties, null, values);

                            String name = glGetProgramResourceName(program, GL_BUFFER_VARIABLE, fieldIndices.get(j), values.get(0));
                            int arrayLength = values.get(1);
                            int offset = values.get(3);
                            int type = values.get(4);
                            if (arrayLength > 1) {
                                int stride = values.get(2);
                                String nameBase = name.substring(0, name.length() - 3);
                                for (int k = 0; k < arrayLength; k++) {
                                    fields.add(new Uniform(nameBase + '[' + k + ']', -1, offset + stride * k, type, 1));
                                }
                            } else {
                                fields.add(new Uniform(name, -1, offset, type, 1));
                            }
                        }
                    } finally {
                        MemoryUtil.memFree(fieldIndices);
                    }

                    Uniform last = fields.getLast();
                    boolean array = last.name.endsWith("[0]");
                    if (array) {
                        fields.set(fields.size() - 1, new Uniform(last.name.substring(0, last.name.length() - 3), last.location, last.offset, last.type, 1));
                    }
                    this.storageBlocks.put(blockName, new StorageBlock(blockName, i, size, values.get(2), fields.toArray(Uniform[]::new)));
                }

                // TODO load atomic counters
            }
        } else {
            int uniformCount = glGetProgrami(program, GL_ACTIVE_UNIFORMS);
            int maxUniformLength = glGetProgrami(program, GL_ACTIVE_UNIFORM_MAX_LENGTH);
            int uniformBlockCount = glGetProgrami(program, GL_ACTIVE_UNIFORM_BLOCKS);
            int maxUniformBlockLength = glGetProgrami(program, GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer size = stack.mallocInt(1);
                IntBuffer type = stack.mallocInt(1);
                for (int i = 0; i < uniformCount; i++) {
                    String baseName = glGetActiveUniform(program, i, maxUniformLength, size, type);

                    // Don't include struct fields
                    if (baseName.contains(".")) {
                        continue;
                    }

                    int length = size.get(0);
                    if (baseName.contains("[")) {
                        baseName = baseName.substring(0, baseName.indexOf('['));
                    }
                    for (int j = 0; j < length; j++) {
                        String name = length > 1 ? baseName + '[' + j + ']' : baseName;
                        Uniform uniform = new Uniform(name, glGetUniformLocation(program, name), 0, type.get(0), 1);
                        this.uniforms.put(name, uniform);
                        if (isSampler(type.get(0))) {
                            this.samplers.put(name, uniform);
                        }
                    }

                    if (length > 1) {
                        Uniform uniform = new Uniform(baseName, glGetUniformLocation(program, baseName), 0, type.get(0), length);
                        this.uniforms.put(baseName, uniform);
                        if (isSampler(type.get(0))) {
                            this.samplers.put(baseName, uniform);
                        }
                    }
                }

                for (int i = 0; i < uniformBlockCount; i++) {
                    String blockName = glGetActiveUniformBlockName(program, i, maxUniformBlockLength);
                    int bufferSize = glGetActiveUniformBlocki(program, i, GL_UNIFORM_BLOCK_DATA_SIZE);

                    Uniform[] fields = new Uniform[glGetActiveUniformBlocki(program, i, GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS)];
                    IntBuffer fieldIndices = MemoryUtil.memAllocInt(fields.length);
                    glGetActiveUniformBlockiv(program, i, GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES, fieldIndices);

                    try {
                        for (int j = 0; j < fields.length; j++) {
                            int index = fieldIndices.get(j);
                            String name = glGetActiveUniform(program, index, maxUniformLength, size, type);
                            int offset = glGetActiveUniformsi(program, index, GL_UNIFORM_OFFSET);
                            fields[j] = new Uniform(name, glGetUniformLocation(program, name), offset, type.get(0), 1);
                        }
                        this.uniformBlocks.put(blockName, new UniformBlock(blockName, i, bufferSize, fields));
                    } finally {
                        MemoryUtil.memFree(fieldIndices);
                    }
                }
            }
        }
    }

    /**
     * Retrieves a uniform by name.
     *
     * @param name The name of the uniform to get
     * @return The uniform found or <code>null</code> if it doesn't exist
     */
    public @Nullable Uniform getUniform(CharSequence name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.uniforms.get(name);
    }

    /**
     * Checks if a uniform exists by the specified name.
     *
     * @param name The name of the uniform to check for
     * @return Whether that uniform exists
     */
    public boolean hasUniform(String name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.uniforms.containsKey(name);
    }

    /**
     * Retrieves a uniform block by name.
     *
     * @param name The name of the uniform block to get
     * @return The uniform block found or <code>null</code> if it doesn't exist
     */
    public @Nullable UniformBlock getUniformBlock(String name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.uniformBlocks.get(name);
    }

    /**
     * Checks if a uniform block exists by the specified name.
     *
     * @param name The name of the uniform block to check for
     * @return Whether that uniform block exists
     */
    public boolean hasUniformBlock(String name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.uniformBlocks.containsKey(name);
    }

    /**
     * Retrieves a storage block by name.
     *
     * @param name The name of the storage block to get
     * @return The storage block found or <code>null</code> if it doesn't exist
     */
    public @Nullable StorageBlock getStorageBlock(String name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.storageBlocks.get(name);
    }

    /**
     * Checks if a storage block exists by the specified name.
     *
     * @param name The name of the storage block to check for
     * @return Whether that storage block exists
     */
    public boolean hasStorageBlock(String name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.storageBlocks.containsKey(name);
    }

    /**
     * Checks if a sampler uniform exists with the specified name.
     *
     * @param name The name of the sampler to check for
     * @return Whether that uniform exists and is a sampler
     */
    public boolean hasSampler(String name) {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.samplers.containsKey(name);
    }

    /**
     * @return A view of all sampler uniforms in the shader
     */
    public Map<String, Uniform> getSamplers() {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.samplersView;
    }

    /**
     * @return A view of all uniforms in the shader
     */
    public Map<String, Uniform> getUniforms() {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.uniformsView;
    }

    /**
     * @return A view of all uniform blocks in the shader
     */
    public Map<String, UniformBlock> getUniformBlocks() {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.uniformBlocksView;
    }

    /**
     * @return A view of all storage blocks in the shader
     */
    public Map<String, StorageBlock> getStorageBlocks() {
        if (!this.requested) {
            this.updateUniforms();
        }
        return this.storageBlocksView;
    }

    /**
     * Checks if the specified GL shader type is a sampler.
     *
     * @param type The type to check
     * @return Whether that GL type is a sampler
     */
    public static boolean isSampler(int type) {
        return SAMPLERS.contains(type);
    }

    /**
     * Retrieves the human-readable name of the specified GL shader type.
     *
     * @param type The type to get the name of
     * @return The human-readable name
     */
    public static String getName(int type) {
        return NAMES.getOrDefault(type, "0x%04X".formatted(type));
    }

    /**
     * A single uniform in a shader program.
     *
     * @param name        The name of the uniform
     * @param location    The uniform location
     * @param offset      The offset of this uniform relative to a containing block
     * @param type        The GL variable type
     * @param arrayLength The number of elements in the array if an array type
     */
    public record Uniform(String name, int location, int offset, int type, int arrayLength) {
    }

    /**
     * A single uniform block in a shader program.
     *
     * @param name   The name of the uniform block
     * @param index  The block index
     * @param size   The size in bytes
     * @param fields All fields in the block
     */
    public record UniformBlock(String name, int index, int size, Uniform[] fields) {
    }

    /**
     * A single uniform block in a shader program.
     *
     * @param name        The name of the uniform block
     * @param index       The block index
     * @param size        The size in bytes
     * @param arrayStride The stride between array elements
     * @param fields      All fields in the block
     */
    public record StorageBlock(String name, int index, int size, int arrayStride, Uniform[] fields) {
        public boolean array() {
            return this.arrayStride > 0;
        }
    }
}
