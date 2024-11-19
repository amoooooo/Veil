package foundry.veil.fabric.ext;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@ApiStatus.Internal
public interface LevelRendererExtension {

    void veil$renderStage(RenderType layer, Matrix4fc frustumMatrix, Matrix4fc projection);
}
