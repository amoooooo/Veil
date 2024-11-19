package foundry.veil.ext;

import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4fc;

@ApiStatus.Internal
public interface LevelRendererBlockLayerExtension {

    void veil$drawBlockLayer(RenderType renderType, double x, double y, double z, Matrix4fc frustum, Matrix4fc projection);
}
