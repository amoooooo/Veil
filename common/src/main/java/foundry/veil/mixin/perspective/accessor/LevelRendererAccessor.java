package foundry.veil.mixin.perspective.accessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor
    Frustum getCullingFrustum();

    @Accessor
    void setCullingFrustum(Frustum frustum);
}
