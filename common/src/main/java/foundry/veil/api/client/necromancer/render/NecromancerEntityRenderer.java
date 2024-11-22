package foundry.veil.api.client.necromancer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.necromancer.animation.Animator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class NecromancerEntityRenderer<T extends Entity & SkeletonParent, M extends Skeleton<T>> extends EntityRenderer<T> {
    final Function<T, M> skeletonFactory;
    final BiFunction<T, M, Animator<T, M>> animatorFactory;
    final List<NecromancerEntityRenderLayer<T, M>> layers;

    protected NecromancerEntityRenderer(EntityRendererProvider.Context pContext,
                                        Function<T, M> skeletonFactory,
                                        BiFunction<T, M, Animator<T, M>> animatorFactory,
                                        float shadowRadius) {
        super(pContext);
        this.skeletonFactory = skeletonFactory;
        this.animatorFactory = animatorFactory;
        this.shadowRadius = shadowRadius;
        this.layers = new ArrayList<>();
    }

    public void addLayer(NecromancerEntityRenderLayer<T, M> layer) {
        this.layers.add(layer);
    }

    public void setupEntity(T entity) {
        M skeleton = skeletonFactory.apply(entity);
        entity.setSkeleton(skeleton);
        entity.setAnimator(animatorFactory.apply(entity, skeleton));
    }

    public abstract Skin<M> getSkin(T parent);

    public void render(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack poseStack, MultiBufferSource pBuffer, int pPackedLight) {
        poseStack.pushPose();
        float scale = 1.0F / 16.0F;
        poseStack.scale(scale, scale, scale);

        Minecraft minecraft = Minecraft.getInstance();
        boolean invisible = pEntity.isInvisibleTo(minecraft.player);
        boolean glowing = minecraft.shouldEntityAppearGlowing(pEntity);

        boolean shouldRender = !invisible;

        RenderType rendertype = this.getRenderType(pEntity, this.getTextureLocation(pEntity));
        M skeleton = (M) pEntity.getSkeleton();
        Skin<M> skin = this.getSkin(pEntity);

        if (shouldRender && skeleton != null && skin != null ) {
            renderSkin(pEntity, skeleton, skin, pEntity.tickCount, pPartialTicks, poseStack, pBuffer.getBuffer(rendertype), pPackedLight, this.getOverlayCoords(pEntity), 1, 1, 1, 1);
        }

        if (!pEntity.isSpectator() && rendertype != null && skeleton != null) {
            for (NecromancerEntityRenderLayer<T, M> layer : this.layers) layer.render(poseStack, pBuffer, pPackedLight, pEntity, skeleton, pPartialTicks);
        }

        poseStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, poseStack, pBuffer, pPackedLight);
    }

    public int getOverlayCoords(T pEntity) {
        return (pEntity instanceof LivingEntity living) ? LivingEntityRenderer.getOverlayCoords(living, 0) : OverlayTexture.NO_OVERLAY;
    }

    public void renderSkin(T entity, M skeleton, Skin<M> skin, int ticksExisted, float partialTicks, PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        skin.render(skeleton, ticksExisted, partialTicks, poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
    }

    public abstract RenderType getRenderType(T entity, ResourceLocation texture);

    protected RenderType getRenderType(T pLivingEntity, boolean visible, boolean spectator, boolean glowing) {
        ResourceLocation texture = this.getTextureLocation(pLivingEntity);
        if (!visible) return null;
        return this.getRenderType(pLivingEntity, texture);
    }
}
