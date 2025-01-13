package foundry.veil.api.client.necromancer.render;

import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.world.entity.Entity;

public abstract class NecromancerEntityRenderLayer<T extends Entity & SkeletonParent<T, M>, M extends Skeleton> {

    public NecromancerEntityRenderer<T, M> renderer;

    public NecromancerEntityRenderLayer(NecromancerEntityRenderer<T, M> renderer) {
        this.renderer = renderer;
    }

    public abstract void render(T entity, M skeleton, NecromancerRenderer renderer, MatrixStack matrixStack, float partialTicks);
}
