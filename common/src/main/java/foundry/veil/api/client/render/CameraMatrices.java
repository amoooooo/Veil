package foundry.veil.api.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import org.joml.*;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL31C.GL_UNIFORM_BUFFER;

/**
 * Packages all camera matrices and shader uniforms to make shader management easier.
 *
 * @author Ocelot
 */
public class CameraMatrices implements NativeResource {

    private static final int SIZE =
            Float.BYTES * 16 +
                    Float.BYTES * 16 +
                    Float.BYTES * 16 +
                    Float.BYTES * 16 +
                    Float.BYTES * 12 +
                    Float.BYTES * 3 +
                    Float.BYTES +
                    Float.BYTES;

    private final ShaderBlock<CameraMatrices> block;
    private final Matrix4f projectionMatrix;
    private final Matrix4f inverseProjectionMatrix;
    private final Matrix4f viewMatrix;
    private final Matrix4f inverseViewMatrix;
    private final Matrix3f inverseViewRotMatrix;
    private final Vector3f cameraPosition;
    private float nearPlane;
    private float farPlane;

    /**
     * Creates a new set of camera matrices.
     */
    public CameraMatrices() {
        this.block = ShaderBlock.withSize(GL_UNIFORM_BUFFER, CameraMatrices.SIZE, CameraMatrices::write);
        this.projectionMatrix = new Matrix4f();
        this.inverseProjectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.inverseViewMatrix = new Matrix4f();
        this.inverseViewRotMatrix = new Matrix3f();
        this.cameraPosition = new Vector3f();
        this.nearPlane = 0.0F;
        this.farPlane = 0.0F;
    }

    private void write(ByteBuffer buffer) {
        this.projectionMatrix.get(0, buffer); // 0
        this.inverseProjectionMatrix.get(Float.BYTES * 16, buffer); // 64
        this.viewMatrix.get(Float.BYTES * 32, buffer); // 128
        this.inverseViewMatrix.get(Float.BYTES * 48, buffer); // 192
        this.inverseViewRotMatrix.get3x4(Float.BYTES * 64, buffer); // 256
        this.cameraPosition.get(Float.BYTES * 76, buffer); // 304
        buffer.putFloat(Float.BYTES * 79, this.nearPlane); // 316
        buffer.putFloat(Float.BYTES * 80, this.farPlane); // 320
    }

    /**
     * Updates the camera matrices to match the specified camera object.
     *
     * @param projection The projection of the camera
     * @param modelView  The modelview rotation of the camera
     * @param pos        The position of the camera
     * @param zFar       The far clipping plane of the camera
     * @param zNear      The near clipping plane of the camera
     */
    public void update(Matrix4fc projection, Matrix4fc modelView, Vector3fc pos, float zNear, float zFar) {
        this.projectionMatrix.set(projection);
        this.projectionMatrix.invert(this.inverseProjectionMatrix);

        // This moves the view bobbing from the projection matrix to the view matrix
        this.viewMatrix.set(modelView).mulLocal(this.inverseProjectionMatrix.mul(RenderSystem.getProjectionMatrix(), new Matrix4f()));
        this.viewMatrix.invert(this.inverseViewMatrix);
        this.inverseViewMatrix.normal(this.inverseViewRotMatrix);

        this.nearPlane = zNear;
        this.farPlane = zFar;
        this.cameraPosition.set(pos);

        this.block.set(this);
        VeilRenderSystem.bind("CameraMatrices", this.block);
    }

    /**
     * Updates the camera matrices to match the current render system projection.
     */
    public void updateGui() {
        this.projectionMatrix.set(RenderSystem.getProjectionMatrix());
        this.projectionMatrix.invert(this.inverseProjectionMatrix);

        this.viewMatrix.identity();
        this.inverseViewMatrix.identity();
        this.inverseViewRotMatrix.identity();

        this.nearPlane = this.inverseProjectionMatrix.transformPosition(0, 0, -1, this.cameraPosition).z();
        this.farPlane = this.inverseProjectionMatrix.transformPosition(0, 0, 1, this.cameraPosition).z();
        this.cameraPosition.set(0);

        this.block.set(this);
        VeilRenderSystem.bind("CameraMatrices", this.block);
    }

    /**
     * Unbinds this shader block.
     */
    public void unbind() {
        VeilRenderSystem.unbind(this.block);
    }

    /**
     * @return The current projection matrix of the camera
     */
    public Matrix4fc getProjectionMatrix() {
        return this.projectionMatrix;
    }

    /**
     * @return The inverse matrix of {@link #getProjectionMatrix()}
     */
    public Matrix4fc getInverseProjectionMatrix() {
        return this.inverseProjectionMatrix;
    }

    /**
     * @return The current view matrix of the camera. This only includes rotation
     */
    public Matrix4fc getViewMatrix() {
        return this.viewMatrix;
    }

    /**
     * @return The inverse matrix of {@link #getViewMatrix()}
     */
    public Matrix4fc getInverseViewMatrix() {
        return this.inverseViewMatrix;
    }

    /**
     * @return The inverse view matrix with only rotation. This is stored as a mat4 to have the correct padding in GLSL
     */
    public Matrix3f getInverseViewRotMatrix() {
        return this.inverseViewRotMatrix;
    }

    /**
     * @return The position of the camera in world space
     */
    public Vector3fc getCameraPosition() {
        return this.cameraPosition;
    }

    /**
     * @return The near clipping plane of the frustum
     */
    public float getNearPlane() {
        return this.nearPlane;
    }

    /**
     * @return The far clipping plane of the frustum
     */
    public float getFarPlane() {
        return this.farPlane;
    }

    @Override
    public void free() {
        this.block.free();
    }
}
