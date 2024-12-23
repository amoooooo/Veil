package foundry.veil.ext;

import org.joml.Matrix4fc;

public interface FrustumExtension {

    void veil$setupFrustum(Matrix4fc frustum, Matrix4fc projection);
}
