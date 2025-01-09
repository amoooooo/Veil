package foundry.veil.api.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.registry.VeilShaderBufferRegistry;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import org.joml.*;

/**
 * Packages all camera matrices and shader uniforms to make shader management easier.
 *
 * @author Ocelot
 */
public class CameraMatrices {

    private final Matrix4f projectionMatrix;
    private final Matrix4f inverseProjectionMatrix;
    private final Matrix4f viewMatrix;
    private final Matrix4f inverseViewMatrix;
    private final Matrix3f inverseViewRotMatrix;
    private final Vector3f cameraPosition;
    private final Vector3f cameraBobOffset;
    private float nearPlane;
    private float farPlane;

    /**
     * Creates a new set of camera matrices.
     */
    public CameraMatrices() {
        this.projectionMatrix = new Matrix4f();
        this.inverseProjectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.inverseViewMatrix = new Matrix4f();
        this.inverseViewRotMatrix = new Matrix3f();
        this.cameraPosition = new Vector3f();
        this.cameraBobOffset = new Vector3f();
        this.nearPlane = 0.0F;
        this.farPlane = 0.0F;
    }

    public static VeilShaderBufferLayout<CameraMatrices> createLayout() {
        return VeilShaderBufferLayout.<CameraMatrices>builder()
                .mat4("ProjMat", CameraMatrices::getProjectionMatrix)
                .mat4("IProjMat", CameraMatrices::getInverseProjectionMatrix)
                .mat4("ViewMat", CameraMatrices::getViewMatrix)
                .mat4("IViewMat", CameraMatrices::getInverseViewMatrix)
                .mat3("IViewRotMat", CameraMatrices::getInverseViewRotMatrix)
                .vec3("CameraPosition", CameraMatrices::getCameraPosition)
                .f32("NearPlane", CameraMatrices::getNearPlane)
                .vec3("CameraBobOffset", CameraMatrices::getCameraBobOffset)
                .f32("FarPlane", CameraMatrices::getFarPlane)
                .build();
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
    public void update(Matrix4fc projection, Matrix4fc modelView, Vector3dc pos, float zNear, float zFar) {
        ShaderBlock<CameraMatrices> block = VeilRenderSystem.getBlock(VeilShaderBufferRegistry.CAMERA.get());
        if (block == null) {
            return;
        }

        this.projectionMatrix.set(projection);
        this.projectionMatrix.invertPerspective(this.inverseProjectionMatrix);

        this.cameraPosition.set((float) pos.x(), (float) pos.y(), (float) pos.z());
        // Adjust the camera position based on the view bobbing
        modelView.invert(this.viewMatrix).transformPosition(VeilRenderSystem.getCameraBobOffset(), this.cameraBobOffset);

        // This moves the view bobbing from the projection matrix to the view matrix
        this.viewMatrix.set(modelView).mulLocal(this.inverseProjectionMatrix.mul(RenderSystem.getProjectionMatrix(), new Matrix4f()));
        this.viewMatrix.invert(this.inverseViewMatrix);
        this.inverseViewMatrix.normal(this.inverseViewRotMatrix);

        this.nearPlane = zNear;
        this.farPlane = zFar;

        block.set(this);
        VeilRenderSystem.bind(VeilShaderBufferRegistry.CAMERA.get());
    }

    /**
     * Updates the camera matrices to match the current render system projection.
     */
    public void updateGui() {
        ShaderBlock<CameraMatrices> block = VeilRenderSystem.getBlock(VeilShaderBufferRegistry.CAMERA.get());
        if (block == null) {
            return;
        }

        this.projectionMatrix.set(RenderSystem.getProjectionMatrix());
        this.projectionMatrix.invertAffine(this.inverseProjectionMatrix);

        this.viewMatrix.identity();
        this.inverseViewMatrix.identity();
        this.inverseViewRotMatrix.identity();

        this.nearPlane = this.inverseProjectionMatrix.transformPosition(0, 0, -1, this.cameraPosition).z();
        this.farPlane = this.inverseProjectionMatrix.transformPosition(0, 0, 1, this.cameraPosition).z();
        this.cameraPosition.set(0);
        this.cameraBobOffset.set(0);

        block.set(this);
        VeilRenderSystem.bind(VeilShaderBufferRegistry.CAMERA.get());
    }

    /**
     * @return The current projection matrix of the camera
     */
    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    /**
     * @return The inverse matrix of {@link #getProjectionMatrix()}
     */
    public Matrix4f getInverseProjectionMatrix() {
        return this.inverseProjectionMatrix;
    }

    /**
     * @return The current view matrix of the camera. This only includes rotation
     */
    public Matrix4f getViewMatrix() {
        return this.viewMatrix;
    }

    /**
     * @return The inverse matrix of {@link #getViewMatrix()}
     */
    public Matrix4f getInverseViewMatrix() {
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
    public Vector3f getCameraPosition() {
        return this.cameraPosition;
    }

    /**
     * @return The world-space offset of the camera due to camera bob
     */
    public Vector3f getCameraBobOffset() {
        return this.cameraBobOffset;
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

    /**
     * Sets the near plane of the projection matrix.
     *
     * @param nearPlane The new near-plane value
     */
    public void setNearPlane(float nearPlane) {
        this.nearPlane = nearPlane;
    }

    /**
     * Sets the far plane of the projection matrix.
     *
     * @param farPlane The new far-plane value
     */
    public void setFarPlane(float farPlane) {
        this.farPlane = farPlane;
    }
}
