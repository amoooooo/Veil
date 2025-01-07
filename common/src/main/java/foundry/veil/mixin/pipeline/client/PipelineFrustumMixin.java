package foundry.veil.mixin.pipeline.client;

import foundry.veil.api.client.render.CullFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

@Mixin(Frustum.class)
public abstract class PipelineFrustumMixin implements CullFrustum {

    @Unique
    private static final MethodHandle PLANES_HANDLE;

    static {
        try {
            PLANES_HANDLE = MethodHandles.privateLookupIn(FrustumIntersection.class, MethodHandles.lookup()).findGetter(FrustumIntersection.class, "planes", Vector4f[].class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Shadow
    @Final
    private FrustumIntersection intersection;

    @Shadow
    @Final
    private Matrix4f matrix;

    @Shadow
    private Vector4f viewVector;

    @Shadow
    private double camX;

    @Shadow
    private double camY;

    @Shadow
    private double camZ;

    @Shadow
    public abstract boolean isVisible(AABB aABB);

    @Shadow
    protected abstract boolean cubeInFrustum(double d, double e, double f, double g, double h, double i);

    @Unique
    private final Vector3d veil$position = new Vector3d();
    @Unique
    private final Vector3f veil$viewVector = new Vector3f();
    @Unique
    private Vector4f[] veil$frustumPlanes = null;

    @Override
    public boolean testPoint(double x, double y, double z) {
        return this.intersection.testPoint((float) (x - this.camX), (float) (y - this.camY), (float) (z - this.camZ));
    }

    @Override
    public boolean testSphere(double x, double y, double z, float r) {
        return this.intersection.testSphere((float) (x - this.camX), (float) (y - this.camY), (float) (z - this.camZ), r);
    }

    @Override
    public boolean testAab(AABB aabb) {
        return this.isVisible(aabb);
    }

    @Override
    public boolean testAab(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.cubeInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean testPlaneXY(double minX, double minY, double maxX, double maxY) {
        return this.intersection.testPlaneXY((float) (minX - this.camX), (float) (minY - this.camY), (float) (maxX - this.camX), (float) (maxY - this.camY));
    }

    @Override
    public boolean testPlaneXZ(double minX, double minZ, double maxX, double maxZ) {
        return this.intersection.testPlaneXZ((float) (minX - this.camX), (float) (minZ - this.camZ), (float) (maxX - this.camX), (float) (maxZ - this.camZ));
    }

    @Override
    public boolean testLineSegment(double aX, double aY, double aZ, double bX, double bY, double bZ) {
        return this.intersection.testLineSegment((float) (aX - this.camX), (float) (aY - this.camY), (float) (aZ - this.camZ), (float) (bX - this.camX), (float) (bY - this.camY), (float) (bZ - this.camZ));
    }

    @Override
    public Vector4fc[] getPlanes() {
        if (this.veil$frustumPlanes == null) {
            try {
                this.veil$frustumPlanes = (Vector4f[]) PLANES_HANDLE.invoke(this.intersection);
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to get frustum planes", t);
            }
        }
        return this.veil$frustumPlanes;
    }

    @Override
    public Vector3dc getPosition() {
        return this.veil$position.set(this.camX, this.camY, this.camZ);
    }

    @Override
    public Matrix4fc getModelViewProjectionMatrix() {
        return this.matrix;
    }

    @Override
    public Vector3fc getViewVector() {
        return this.veil$viewVector.set(this.viewVector.x, this.viewVector.y, this.viewVector.z).normalize();
    }

    @Override
    public Frustum toFrustum() {
        return (Frustum) (Object) this;
    }
}
