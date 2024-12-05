package foundry.veil.api.client.render.shader.program;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import org.joml.*;

import java.lang.Math;

/**
 * Provides write access to all uniform variables in a shader program.
 *
 * @author Ocelot
 */
public interface MutableUniformAccess extends UniformAccess {

    /**
     * Sets default uniforms based on what {@link RenderSystem} provides.
     */
    default void applyRenderSystem() {
        this.setMatrix("ModelViewMat", RenderSystem.getModelViewMatrix());
        this.setMatrix("ProjMat", RenderSystem.getProjectionMatrix());
        this.setVector("ColorModulator", RenderSystem.getShaderColor());
        this.setFloat("GlintAlpha", RenderSystem.getShaderGlintAlpha());
        this.setFloat("FogStart", RenderSystem.getShaderFogStart());
        this.setFloat("FogEnd", RenderSystem.getShaderFogEnd());
        this.setVector("FogColor", RenderSystem.getShaderFogColor());
        this.setInt("FogShape", RenderSystem.getShaderFogShape().getIndex());
        this.setMatrix("TextureMatrix", RenderSystem.getTextureMatrix());
        this.setFloat("GameTime", RenderSystem.getShaderGameTime());

        // TODO move to uniform block
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            for (Direction value : Direction.values()) {
                this.setFloat("VeilBlockFaceBrightness[" + value.get3DDataValue() + "]", level.getShade(value, true));
            }
        }
    }

    /**
     * Sets the binding to use for the specified uniform block.
     *
     * @param name    The name of the block to set
     * @param binding The binding to use for that block
     */
    void setUniformBlock(CharSequence name, int binding);

    /**
     * Sets the binding to use for the specified storage block.
     *
     * @param name    The name of the block to set
     * @param binding The binding to use for that block
     */
    void setStorageBlock(CharSequence name, int binding);

    /**
     * Sets a float in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    void setFloat(CharSequence name, float value);

    /**
     * Sets a vector in the shader.
     *
     * @param name The name of the uniform to set
     * @param x    The x component of the vector
     * @param y    The y component of the vector
     */
    void setVector(CharSequence name, float x, float y);

    /**
     * Sets a vector in the shader.
     *
     * @param name The name of the uniform to set
     * @param x    The x component of the vector
     * @param y    The y component of the vector
     * @param z    The z component of the vector
     */
    void setVector(CharSequence name, float x, float y, float z);

    /**
     * Sets a vector in the shader.
     *
     * @param name The name of the uniform to set
     * @param x    The x component of the vector
     * @param y    The y component of the vector
     * @param z    The z component of the vector
     * @param w    The w component of the vector
     */
    void setVector(CharSequence name, float x, float y, float z, float w);

    /**
     * Sets a vector in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setVector(CharSequence name, Vector2fc value) {
        this.setVector(name, value.x(), value.y());
    }

    /**
     * Sets a vector in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setVector(CharSequence name, Vector3fc value) {
        this.setVector(name, value.x(), value.y(), value.z());
    }

    /**
     * Sets a vector in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setVector(CharSequence name, Vector4fc value) {
        this.setVector(name, value.x(), value.y(), value.z(), value.w());
    }

    /**
     * Sets a vector in the shader.
     *
     * @param name   The name of the uniform to set
     * @param values The values to set
     * @throws UnsupportedOperationException If the array passed in is empty
     */
    default void setVector(CharSequence name, float[] values) {
        switch (Math.min(4, values.length)) {
            case 1 -> this.setFloat(name, values[0]);
            case 2 -> this.setVector(name, values[0], values[1]);
            case 3 -> this.setVector(name, values[0], values[1], values[2]);
            case 4 -> this.setVector(name, values[0], values[1], values[2], values[3]);
            default -> throw new UnsupportedOperationException("At least 1 value must be specified");
        }
    }

    /**
     * Sets an integer in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    void setInt(CharSequence name, int value);

    /**
     * Sets an integer vector in the shader.
     *
     * @param name The name of the uniform to set
     * @param x    The x component of the vector
     * @param y    The y component of the vector
     */
    void setVectorI(CharSequence name, int x, int y);

    /**
     * Sets an integer vector in the shader.
     *
     * @param name The name of the uniform to set
     * @param x    The x component of the vector
     * @param y    The y component of the vector
     * @param z    The z component of the vector
     */
    void setVectorI(CharSequence name, int x, int y, int z);

    /**
     * Sets an integer vector in the shader.
     *
     * @param name The name of the uniform to set
     * @param x    The x component of the vector
     * @param y    The y component of the vector
     * @param z    The z component of the vector
     * @param w    The w component of the vector
     */
    void setVectorI(CharSequence name, int x, int y, int z, int w);

    /**
     * Sets an integer vector in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setVectorI(CharSequence name, Vector2ic value) {
        this.setVectorI(name, value.x(), value.y());
    }

    /**
     * Sets an integer vector in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setVectorI(CharSequence name, Vector3ic value) {
        this.setVectorI(name, value.x(), value.y(), value.z());
    }

    /**
     * Sets an integer vector in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setVectorI(CharSequence name, Vector4ic value) {
        this.setVectorI(name, value.x(), value.y(), value.z(), value.w());
    }

    /**
     * Sets an array of floats in the shader.
     *
     * @param name   The name of the uniform to set
     * @param values The values to set in order
     */
    void setFloats(CharSequence name, float... values);

    /**
     * Sets an array of vectors in the shader.
     *
     * @param name   The name of the uniform to set
     * @param values The values to set in order
     */
    void setVectors(CharSequence name, Vector2fc... values);

    /**
     * Sets an array of vectors in the shader.
     *
     * @param name   The name of the uniform to set
     * @param values The values to set in order
     */
    void setVectors(CharSequence name, Vector3fc... values);

    /**
     * Sets an array of vectors in the shader.
     *
     * @param name   The name of the uniform to set
     * @param values The values to set in order
     */
    void setVectors(CharSequence name, Vector4fc... values);

    /**
     * Sets an array of integers in the shader.
     *
     * @param name   The name of the uniform to set
     * @param values The values to set in order
     */
    void setInts(CharSequence name, int... values);

    /**
     * Sets an array of integer vectors in the shader.
     *
     * @param name   The name of the uniform to set
     * @param values The values to set in order
     */
    void setVectors(CharSequence name, Vector2ic... values);

    /**
     * Sets an array of integer vectors in the shader.
     *
     * @param name   The name of the uniform to set
     * @param values The values to set in order
     */
    void setVectors(CharSequence name, Vector3ic... values);

    /**
     * Sets an array of integer vectors in the shader.
     *
     * @param name   The name of the uniform to set
     * @param values The values to set in order
     */
    void setVectors(CharSequence name, Vector4ic... values);

    /**
     * Sets a matrix in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setMatrix(CharSequence name, Matrix2fc value) {
        this.setMatrix(name, value, false);
    }

    /**
     * Sets a matrix in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setMatrix(CharSequence name, Matrix3fc value) {
        this.setMatrix(name, value, false);
    }

    /**
     * Sets a matrix in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setMatrix(CharSequence name, Matrix3x2fc value) {
        this.setMatrix(name, value, false);
    }

    /**
     * Sets a matrix in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setMatrix(CharSequence name, Matrix4fc value) {
        this.setMatrix(name, value, false);
    }

    /**
     * Sets a matrix in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    default void setMatrix(CharSequence name, Matrix4x3fc value) {
        this.setMatrix(name, value, false);
    }

    /**
     * Sets a matrix in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    void setMatrix(CharSequence name, Matrix2fc value, boolean transpose);

    /**
     * Sets a matrix in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    void setMatrix(CharSequence name, Matrix3fc value, boolean transpose);

    /**
     * Sets a matrix in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    void setMatrix(CharSequence name, Matrix3x2fc value, boolean transpose);

    /**
     * Sets a matrix in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    void setMatrix(CharSequence name, Matrix4fc value, boolean transpose);

    /**
     * Sets a matrix in the shader.
     *
     * @param name  The name of the uniform to set
     * @param value The value to set
     */
    void setMatrix(CharSequence name, Matrix4x3fc value, boolean transpose);
}
