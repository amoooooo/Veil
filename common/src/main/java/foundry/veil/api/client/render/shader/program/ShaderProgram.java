package foundry.veil.api.client.render.shader.program;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.compiler.CompiledShader;
import foundry.veil.impl.client.render.shader.program.ShaderProgramImpl;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Set;

import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL31C.GL_INVALID_INDEX;
import static org.lwjgl.opengl.GL31C.glUniformBlockBinding;
import static org.lwjgl.opengl.GL41C.*;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43C.glShaderStorageBlockBinding;

/**
 * Represents a usable shader program with shaders attached.
 *
 * @author Ocelot
 */
public interface ShaderProgram extends NativeResource, MutableUniformAccess, TextureUniformAccess {

    /**
     * Binds this program for use.
     */
    default void bind() {
        int program = this.getProgram();
        if (ShaderInstance.lastProgramId != program) {
            ShaderInstance.lastProgramId = program;
            GlStateManager._glUseProgram(program);
        }
        EffectInstance.lastProgramId = -1;
    }

    /**
     * Unbinds the currently bound shader program.
     */
    static void unbind() {
        VeilRenderSystem.clearShaderBlocks();
        GlStateManager._glUseProgram(0);
        ShaderInstance.lastProgramId = -1;
        EffectInstance.lastProgramId = -1;
        VeilRenderSystem.unbindSamplers(0, VeilRenderSystem.maxCombinedTextureUnits());
        ShaderProgramImpl.restoreBlendState();
    }

    /**
     * Sets the default uniforms in this shader.
     *
     * @param mode The expected draw mode
     */
    default void setDefaultUniforms(VertexFormat.Mode mode) {
        this.setDefaultUniforms(mode, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
    }

    /**
     * Sets the default uniforms in this shader.
     *
     * @param mode             The expected draw mode
     * @param modelViewMatrix  The view matrix transform
     * @param projectionMatrix The projection matrix transform
     */
    void setDefaultUniforms(VertexFormat.Mode mode, Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix);

    /**
     * @return The OpenGL id of this program
     */
    int getProgram();

    @Override
    default float getFloat(CharSequence name) {
        return glGetUniformf(this.getProgram(), this.getUniform(name));
    }

    @Override
    default int getInt(CharSequence name) {
        return glGetUniformi(this.getProgram(), this.getUniform(name));
    }

    @Override
    default void getFloats(CharSequence name, float[] values) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length);
            glGetUniformfv(this.getProgram(), this.getUniform(name), buffer);
            for (int i = 0; i < values.length; i++) {
                values[i] = buffer.get(i);
            }
        }
    }

    @Override
    default void getVector(CharSequence name, Vector2f... values) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length * 2);
            glGetUniformfv(this.getProgram(), this.getUniform(name), buffer);
            for (int i = 0; i < values.length; i++) {
                values[i].set(i * 2, buffer);
            }
        }
    }

    @Override
    default void getVector(CharSequence name, Vector3f... values) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length * 3);
            glGetUniformfv(this.getProgram(), this.getUniform(name), buffer);
            for (int i = 0; i < values.length; i++) {
                values[i].set(i * 3, buffer);
            }
        }
    }

    @Override
    default void getVector(CharSequence name, Vector4f... values) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length * 4);
            glGetUniformfv(this.getProgram(), this.getUniform(name), buffer);
            for (int i = 0; i < values.length; i++) {
                values[i].set(i * 4, buffer);
            }
        }
    }

    @Override
    default void getInts(CharSequence name, int[] values) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(values.length);
            glGetUniformiv(this.getProgram(), this.getUniform(name), buffer);
            for (int i = 0; i < values.length; i++) {
                values[i] = buffer.get(i);
            }
        }
    }

    @Override
    default void getVector(CharSequence name, Vector2i... values) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(values.length * 2);
            glGetUniformiv(this.getProgram(), this.getUniform(name), buffer);
            for (int i = 0; i < values.length; i++) {
                values[i].set(i * 2, buffer);
            }
        }
    }

    @Override
    default void getVector(CharSequence name, Vector3i... values) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(values.length * 3);
            glGetUniformiv(this.getProgram(), this.getUniform(name), buffer);
            for (int i = 0; i < values.length; i++) {
                values[i].set(i * 3, buffer);
            }
        }
    }

    @Override
    default void getVector(CharSequence name, Vector4i... values) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(values.length * 4);
            glGetUniformiv(this.getProgram(), this.getUniform(name), buffer);
            for (int i = 0; i < values.length; i++) {
                values[i].set(i * 4, buffer);
            }
        }
    }

    @Override
    default void getMatrix(CharSequence name, Matrix2f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(2 * 2);
            glGetUniformfv(this.getProgram(), this.getUniform(name), buffer);
            value.set(0, buffer);
        }
    }

    @Override
    default void getMatrix(CharSequence name, Matrix3f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(3 * 3);
            glGetUniformfv(this.getProgram(), this.getUniform(name), buffer);
            value.set(0, buffer);
        }
    }

    @Override
    default void getMatrix(CharSequence name, Matrix3x2f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(3 * 2);
            glGetUniformfv(this.getProgram(), this.getUniform(name), buffer);
            value.set(0, buffer);
        }
    }

    @Override
    default void getMatrix(CharSequence name, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(4 * 4);
            glGetUniformfv(this.getProgram(), this.getUniform(name), buffer);
            value.set(0, buffer);
        }
    }

    @Override
    default void getMatrix(CharSequence name, Matrix4x3f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(4 * 3);
            glGetUniformfv(this.getProgram(), this.getUniform(name), buffer);
            value.set(0, buffer);
        }
    }

    @Override
    default void setUniformBlock(CharSequence name, int binding) {
        int index = this.getUniformBlock(name);
        if (index != GL_INVALID_INDEX) {
            glUniformBlockBinding(this.getProgram(), index, binding);
        }
    }

    @Override
    default void setStorageBlock(CharSequence name, int binding) {
        int index = this.getStorageBlock(name);
        if (index != GL_INVALID_INDEX) {
            glShaderStorageBlockBinding(this.getProgram(), index, binding);
        }
    }

    @Override
    default void setFloat(CharSequence name, float value) {
        int location = this.getUniform(name);
        if (location != -1) {
            glProgramUniform1f(this.getProgram(), location, value);
        }
    }

    @Override
    default void setVector(CharSequence name, float x, float y) {
        int location = this.getUniform(name);
        if (location != -1) {
            glProgramUniform2f(this.getProgram(), location, x, y);
        }
    }

    @Override
    default void setVector(CharSequence name, float x, float y, float z) {
        int location = this.getUniform(name);
        if (location != -1) {
            glProgramUniform3f(this.getProgram(), location, x, y, z);
        }
    }

    @Override
    default void setVector(CharSequence name, float x, float y, float z, float w) {
        int location = this.getUniform(name);
        if (location != -1) {
            glProgramUniform4f(this.getProgram(), location, x, y, z, w);
        }
    }

    @Override
    default void setInt(CharSequence name, int value) {
        int location = this.getUniform(name);
        if (location != -1) {
            glProgramUniform1i(this.getProgram(), location, value);
        }
    }

    @Override
    default void setVectorI(CharSequence name, int x, int y) {
        int location = this.getUniform(name);
        if (location != -1) {
            glProgramUniform2i(this.getProgram(), location, x, y);
        }
    }

    @Override
    default void setVectorI(CharSequence name, int x, int y, int z) {
        int location = this.getUniform(name);
        if (location != -1) {
            glProgramUniform3i(this.getProgram(), location, x, y, z);
        }
    }

    @Override
    default void setVectorI(CharSequence name, int x, int y, int z, int w) {
        int location = this.getUniform(name);
        if (location != -1) {
            glProgramUniform4i(this.getProgram(), location, x, y, z, w);
        }
    }

    @Override
    default void setFloats(CharSequence name, float... values) {
        int location = this.getUniform(name);
        if (location != -1) {
            glProgramUniform1fv(this.getProgram(), location, values);
        }
    }

    @Override
    default void setVectors(CharSequence name, Vector2fc... values) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length * 2);
            for (int i = 0; i < values.length; i++) {
                values[i].get(i * 2, buffer);
            }
            glProgramUniform2fv(this.getProgram(), location, buffer);
        }
    }

    @Override
    default void setVectors(CharSequence name, Vector3fc... values) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length * 3);
            for (int i = 0; i < values.length; i++) {
                values[i].get(i * 3, buffer);
            }
            glProgramUniform3fv(this.getProgram(), location, buffer);
        }
    }

    @Override
    default void setVectors(CharSequence name, Vector4fc... values) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length * 4);
            for (int i = 0; i < values.length; i++) {
                values[i].get(i * 4, buffer);
            }
            glProgramUniform4fv(this.getProgram(), location, buffer);
        }
    }

    @Override
    default void setInts(CharSequence name, int... values) {
        int location = this.getUniform(name);
        if (location != -1) {
            glProgramUniform1iv(this.getProgram(), location, values);
        }
    }

    @Override
    default void setVectors(CharSequence name, Vector2ic... values) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(values.length * 2);
            for (int i = 0; i < values.length; i++) {
                values[i].get(i * 2, buffer);
            }
            glProgramUniform2iv(this.getProgram(), location, buffer);
        }
    }

    @Override
    default void setVectors(CharSequence name, Vector3ic... values) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(values.length * 3);
            for (int i = 0; i < values.length; i++) {
                values[i].get(i * 3, buffer);
            }
            glProgramUniform3iv(this.getProgram(), location, buffer);
        }
    }

    @Override
    default void setVectors(CharSequence name, Vector4ic... values) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(values.length * 4);
            for (int i = 0; i < values.length; i++) {
                values[i].get(i * 4, buffer);
            }
            glProgramUniform4iv(this.getProgram(), location, buffer);
        }
    }

    @Override
    default void setMatrix(CharSequence name, Matrix2fc value, boolean transpose) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(2 * 2);
            value.get(buffer);
            glProgramUniformMatrix2fv(this.getProgram(), location, transpose, buffer);
        }
    }

    @Override
    default void setMatrix(CharSequence name, Matrix3fc value, boolean transpose) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(3 * 3);
            value.get(buffer);
            glProgramUniformMatrix3fv(this.getProgram(), location, transpose, buffer);
        }
    }

    @Override
    default void setMatrix(CharSequence name, Matrix3x2fc value, boolean transpose) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(3 * 2);
            value.get(buffer);
            glProgramUniformMatrix3x2fv(this.getProgram(), location, transpose, buffer);
        }
    }

    @Override
    default void setMatrix(CharSequence name, Matrix4fc value, boolean transpose) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(4 * 4);
            value.get(buffer);
            glProgramUniformMatrix4fv(this.getProgram(), location, transpose, buffer);
        }
    }

    @Override
    default void setMatrix(CharSequence name, Matrix4x3fc value, boolean transpose) {
        int location = this.getUniform(name);
        if (location == -1) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(4 * 3);
            value.get(buffer);
            glProgramUniformMatrix4x3fv(this.getProgram(), location, transpose, buffer);
        }
    }

    /**
     * @return The definition used to compile the latest version of this shader
     */
    @Nullable
    ProgramDefinition getDefinition();

    /**
     * @return The shaders attached to this program
     */
    Int2ObjectMap<CompiledShader> getShaders();

    /**
     * @return Whether this program has the vertex stage
     */
    default boolean hasVertex() {
        return this.getShaders().containsKey(GL_VERTEX_SHADER);
    }

    /**
     * @return Whether this program has the geometry stage
     */
    default boolean hasGeometry() {
        return this.getShaders().containsKey(GL_GEOMETRY_SHADER);
    }

    /**
     * @return Whether this program has the fragment stage
     */
    default boolean hasFragment() {
        return this.getShaders().containsKey(GL_VERTEX_SHADER);
    }

    /**
     * @return Whether this program has the tesselation stages
     */
    default boolean hasTesselation() {
        Int2ObjectMap<CompiledShader> shaders = this.getShaders();
        return shaders.containsKey(GL_TESS_CONTROL_SHADER) && shaders.containsKey(GL_TESS_EVALUATION_SHADER);
    }

    /**
     * @return Whether this program has the compute stage
     */
    default boolean isCompute() {
        return this.getShaders().containsKey(GL_COMPUTE_SHADER);
    }

    /**
     * @return A guess at the best vertex format for this program
     */
    @Nullable
    VertexFormat getFormat();

    /**
     * @return All shader definitions this program depends on
     */
    Set<String> getDefinitionDependencies();

    /**
     * @return The name of this program
     */
    ResourceLocation getName();

    /**
     * <p>Wraps this shader with a vanilla Minecraft shader instance wrapper. There are a few special properties about the shader wrapper.</p>
     * <ul>
     *     <li>The shader instance cannot be used to free the shader program. {@link ShaderProgram#free()} must be called separately.
     *     If the shader is loaded through {@link ShaderManager} then there is no need to free the shader.</li>
     *     <li>Calling {@link Uniform#upload()} will do nothing since the values are uploaded when the appropriate methods are called</li>
     *     <li>Uniforms are lazily wrapped and will not crash when the wrong method is called.</li>
     *     <li>{@link Uniform#set(int, float)} is not supported and will throw an {@link UnsupportedOperationException}.</li>
     *     <li>{@link Uniform#set(float[])} only works for 1, 2, 3, and 4 float elements. Any other size will throw an {@link UnsupportedOperationException}.</li>
     * </ul>
     *
     * @return A lazily loaded shader instance wrapper for this program
     */
    ShaderInstance toShaderInstance();
}
