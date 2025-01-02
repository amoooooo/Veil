package foundry.veil.api.client.necromancer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

public abstract class NecromancerEntityRenderLayer<T extends Entity & SkeletonParent<T, M>, M extends Skeleton<T>> {

    public NecromancerEntityRenderer<T, M> renderer;

    public NecromancerEntityRenderLayer(NecromancerEntityRenderer<T, M> pRenderer) {
        this.renderer = pRenderer;
    }

    public abstract void render(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, T pLivingEntity, M pSkeleton, float pPartialTicks);
}
