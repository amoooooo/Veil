package foundry.veil.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaterniondc;
import org.joml.Quaternionfc;

/**
 * Utility extension for {@link PoseStack} with extra utilities.
 *
 * @see VeilRenderBridge
 */
public interface MatrixStack {

    /**
     * Clears all transformations.
     */
    void clear();

    /**
     * Translates the position by the specified amount.
     *
     * @param x The x amount
     * @param y The y amount
     * @param z The z amount
     */
    default void translate(double x, double y, double z) {
        this.translate((float) x, (float) y, (float) z);
    }

    /**
     * Translates the position by the specified amount.
     *
     * @param x The x amount
     * @param y The y amount
     * @param z The z amount
     */
    void translate(float x, float y, float z);

    /**
     * Rotates the position and normal by the specified quaternion rotation.
     *
     * @param rotation The rotation to use
     */
    void rotate(Quaterniondc rotation);

    /**
     * Rotates the position and normal by the specified quaternion rotation.
     *
     * @param rotation The rotation to use
     */
    void rotate(Quaternionfc rotation);

    /**
     * <p>Rotates the position and normal by the specified angle about the line specified by x, y, z.</p>
     * <p>For rotating along all 3 axes, use {@link #rotateXYZ(double, double, double)} or {@link #rotateZYX(double, double, double)}.</p>
     *
     * @param angle The amount to rotate in radians
     * @param x     The x normal
     * @param y     The y normal
     * @param z     The z normal
     */
    default void rotate(double angle, double x, double y, double z) {
        this.rotate((float) angle, (float) x, (float) y, (float) z);
    }

    /**
     * <p>Rotates the position and normal by the specified angle about the line specified by x, y, z.</p>
     * <p>For rotating along all 3 axes, use {@link #rotateXYZ(float, float, float)} or {@link #rotateZYX(float, float, float)}.</p>
     *
     * @param angle The amount to rotate in radians
     * @param x     The x normal
     * @param y     The y normal
     * @param z     The z normal
     */
    void rotate(float angle, float x, float y, float z);

    /**
     * Rotates about the x, y, then z planes the specified angles.
     *
     * @param x The amount to rotate in the x in radians
     * @param y The amount to rotate in the y in radians
     * @param z The amount to rotate in the z in radians
     */
    default void rotateXYZ(double x, double y, double z) {
        this.rotateXYZ((float) x, (float) y, (float) z);
    }

    /**
     * Rotates about the x, y, then z planes the specified angles.
     *
     * @param x The amount to rotate in the x in radians
     * @param y The amount to rotate in the y in radians
     * @param z The amount to rotate in the z in radians
     */
    void rotateXYZ(float x, float y, float z);

    /**
     * Rotates about the z, y, then x planes the specified angles.
     *
     * @param z The amount to rotate in the z in radians
     * @param y The amount to rotate in the y in radians
     * @param x The amount to rotate in the x in radians
     */
    default void rotateZYX(double z, double y, double x) {
        this.rotateZYX((float) z, (float) y, (float) x);
    }

    /**
     * Rotates about the z, y, then x planes the specified angles.
     *
     * @param z The amount to rotate in the z in radians
     * @param y The amount to rotate in the y in radians
     * @param x The amount to rotate in the x in radians
     */
    void rotateZYX(float x, float y, float z);

    /**
     * Rotates the position and normal by the specified quaternion rotation about the specified rotation point.
     *
     * @param rotation The rotation to use
     * @param x        The rotation point X
     * @param y        The rotation point Y
     * @param z        The rotation point Z
     */
    void rotateAround(Quaterniondc rotation, double x, double y, double z);

    /**
     * Rotates the position and normal by the specified quaternion rotation about the specified rotation point.
     *
     * @param rotation The rotation to use
     * @param x        The rotation point X
     * @param y        The rotation point Y
     * @param z        The rotation point Z
     */
    void rotateAround(Quaternionfc rotation, float x, float y, float z);

    /**
     * Scales the position and normal by the specified amount in the x, y, and z.
     *
     * @param xyz The scale factor
     */
    default void applyScale(double xyz) {
        this.applyScale((float) xyz, (float) xyz, (float) xyz);
    }

    /**
     * Scales the position and normal by the specified amount in the x, y, and z.
     *
     * @param xyz The scale factor
     */
    default void applyScale(float xyz) {
        this.applyScale(xyz, xyz, xyz);
    }

    /**
     * Scales the position and normal by the specified amount in the x, y, and z.
     *
     * @param x The x scale factor
     * @param y The y scale factor
     * @param z The z scale factor
     */
    default void applyScale(double x, double y, double z) {
        this.applyScale((float) x, (float) y, (float) z);
    }

    /**
     * Scales the position and normal by the specified amount in the x, y, and z.
     *
     * @param x The x scale factor
     * @param y The y scale factor
     * @param z The z scale factor
     */
    void applyScale(float x, float y, float z);

    /**
     * Copies the current transformation of the specified stack into the current transformation of this stack.
     *
     * @param other The stack to copy
     */
    default void copy(PoseStack other) {
        this.copy(other.last());
    }

    /**
     * Copies the current transformation of the specified stack into the current transformation of this stack.
     *
     * @param other The stack to copy
     */
    default void copy(MatrixStack other) {
        this.copy(other.pose());
    }

    /**
     * Copies the current transformation of the specified pose into the current transformation of this stack.
     *
     * @param other The pose to copy
     */
    default void copy(PoseStack.Pose other) {
        PoseStack.Pose pose = this.pose();
        pose.pose().set(other.pose());
        pose.normal().set(other.normal());
    }

    /**
     * Sets the current transformation and normal to identity.
     */
    default void setIdentity() {
        PoseStack.Pose pose = this.pose();
        pose.pose().identity();
        pose.normal().identity();
    }

    /**
     * @return Whether the {@link #position()} and {@link #normal()} are identity matrices
     */
    boolean isIdentity();

    /**
     * @return Whether there are no more transformations to pop
     */
    boolean isEmpty();

    /**
     * Saves the current position and normal transformation for restoring later with {@link #matrixPop()}.
     */
    void matrixPush();

    /**
     * Restores a previous position and normal set with {@link #matrixPush()}.
     *
     * @throws IllegalStateException If there are no more matrix transformations to pop
     */
    void matrixPop();

    /**
     * @return The last pose in the stack
     */
    PoseStack.Pose pose();

    /**
     * @return The current position matrix
     */
    default Matrix4f position() {
        return this.pose().pose();
    }

    /**
     * @return The computed normal matrix from the position
     */
    default Matrix3f normal() {
        return this.pose().normal();
    }

    /**
     * @return This matrix stack as a vanilla mc {@link PoseStack}
     */
    PoseStack toPoseStack();
}
