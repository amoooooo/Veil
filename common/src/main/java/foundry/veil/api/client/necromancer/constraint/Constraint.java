package foundry.veil.api.client.necromancer.constraint;

import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import net.minecraft.client.renderer.MultiBufferSource;

public interface Constraint {

    void apply();

    default void renderDebugInfo(Skeleton skeleton, SkeletonParent<?, ?> parent, float pPartialTicks, PoseStack poseStack, MultiBufferSource pBuffer) {
    }
}
