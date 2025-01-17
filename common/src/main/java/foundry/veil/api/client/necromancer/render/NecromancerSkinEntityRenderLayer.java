package foundry.veil.api.client.necromancer.render;

import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public abstract class NecromancerSkinEntityRenderLayer<T extends Entity & SkeletonParent<T, M>, M extends Skeleton> extends NecromancerEntityRenderLayer<T, M> {

    public NecromancerSkinEntityRenderLayer(NecromancerEntityRenderer<T, M> renderer) {
        super(renderer);
    }

    public abstract @Nullable RenderType getRenderType(T entity);

    public abstract Skin getSkin(T parent);

    @Override
    public void render(T entity, M skeleton, NecromancerRenderer renderer, MatrixStack matrixStack, int packedLight, float partialTicks) {
        RenderType renderType = this.getRenderType(entity);
        if (renderType != null) {
            renderer.setTransform(matrixStack.position());
            renderer.setLight(packedLight);
            renderer.draw(renderType, skeleton, this.getSkin(entity), partialTicks);
            renderer.reset();
        }
    }
}
