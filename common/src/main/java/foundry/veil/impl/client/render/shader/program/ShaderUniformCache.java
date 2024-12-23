package foundry.veil.impl.client.render.shader.program;

import foundry.veil.api.client.render.shader.program.ShaderProgram;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL31C.*;
import static org.lwjgl.opengl.GL32C.*;
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
                if (SAMPLERS.contains(type.get(0))) {
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
}
