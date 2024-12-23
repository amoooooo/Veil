package foundry.veil.mixin.accessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor
    @Nullable
    PostChain getEntityEffect();

    @Accessor
    @Nullable
    PostChain getTransparencyChain();

    @Accessor
    Frustum getCullingFrustum();

    @Accessor
    void setCullingFrustum(Frustum frustum);
}
