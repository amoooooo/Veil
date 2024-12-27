package foundry.veil.ext;

import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4fc;

@ApiStatus.Internal
public interface FrustumExtension {

    void veil$setupFrustum(Matrix4fc frustum, Matrix4fc projection);
}
