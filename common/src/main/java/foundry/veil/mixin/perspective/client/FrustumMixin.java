package foundry.veil.mixin.perspective.client;

import foundry.veil.ext.FrustumExtension;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Frustum.class)
public class FrustumMixin implements FrustumExtension {

    @Shadow
    @Final
    private Matrix4f matrix;

    @Shadow
    @Final
    private FrustumIntersection intersection;

    @Shadow
    private Vector4f viewVector;

    @Override
    public void veil$setupFrustum(Matrix4fc frustum, Matrix4fc projection) {
        projection.mul(frustum, this.matrix);
        this.intersection.set(this.matrix);
        this.matrix.transformTranspose(this.viewVector.set(0.0F, 0.0F, 1.0F, 0.0F));
    }
}
