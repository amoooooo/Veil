package foundry.veil.mixin.accessor;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Frustum.class)
public interface FrustumAccessor {

    @Accessor
    Matrix4f getMatrix();

    @Accessor
    double getCamX();

    @Accessor
    double getCamY();

    @Accessor
    double getCamZ();

    @Invoker
    void invokeCalculateFrustum(Matrix4f frustum, Matrix4f projection);
}
