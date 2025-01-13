package foundry.veil.api.client.necromancer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.necromancer.animation.Animator;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.impl.client.necromancer.render.NecromancerRenderDispatcher;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;

public abstract class NecromancerEntityRenderer<P extends Entity & SkeletonParent<P, S>, S extends Skeleton> extends EntityRenderer<P> {

    private final List<NecromancerEntityRenderLayer<P, S>> layers;

    protected NecromancerEntityRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context);
        this.shadowRadius = shadowRadius;
        this.layers = new ObjectArrayList<>();
    }

    public void addLayer(NecromancerEntityRenderLayer<P, S> layer) {
        this.layers.add(layer);
    }

    public final void setupEntity(P parent) {
        S skeleton = this.createSkeleton(parent);
        parent.setSkeleton(skeleton);
        parent.setAnimator(this.createAnimator(parent, skeleton));
    }

    public abstract S createSkeleton(P parent);

    public abstract Animator<P, S> createAnimator(P parent, S skeleton);

    @Override
    public void render(P parent, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        NecromancerRenderer renderer = NecromancerRenderDispatcher.getRenderer();
        this.render(parent, renderer, VeilRenderBridge.create(poseStack), partialTick);
        super.render(parent, entityYaw, partialTick, poseStack, renderer, packedLight);
    }

    public void render(P parent, NecromancerRenderer context, MatrixStack matrixStack, float partialTicks) {
        S skeleton = parent.getSkeleton();
        if (skeleton == null) {
            return;
        }

        matrixStack.matrixPush();
        matrixStack.applyScale(0.0625F);
        for (NecromancerEntityRenderLayer<P, S> layer : this.layers) {
            layer.render(parent, skeleton, context, matrixStack, partialTicks);
        }
        matrixStack.matrixPop();
    }

    @Override
    public ResourceLocation getTextureLocation(P entity) {
        throw new UnsupportedOperationException("");
    }
}
