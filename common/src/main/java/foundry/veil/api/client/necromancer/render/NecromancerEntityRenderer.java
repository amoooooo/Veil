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

/**
 * A renderer for a necromancer entity.
 *
 * @param <P> The entity to render
 * @param <S> The skeleton for the entity
 */
public abstract class NecromancerEntityRenderer<P extends Entity & SkeletonParent<P, S>, S extends Skeleton> extends EntityRenderer<P> {

    private final List<NecromancerEntityRenderLayer<P, S>> layers;

    protected NecromancerEntityRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context);
        this.shadowRadius = shadowRadius;
        this.layers = new ObjectArrayList<>();
    }

    /**
     * Adds a new layer to this renderer.
     *
     * @param layer The layer to add
     */
    public void addLayer(NecromancerEntityRenderLayer<P, S> layer) {
        this.layers.add(layer);
    }

    /**
     * Adds a skeleton and animator to the specified entity.
     *
     * @param parent The entity to set up
     */
    public final void setupEntity(P parent) {
        S skeleton = this.createSkeleton(parent);
        parent.setSkeleton(skeleton);
        parent.setAnimator(this.createAnimator(parent, skeleton));
    }

    /**
     * Creates a skeleton for the specified entity.
     *
     * @param parent The entity to create the skeleton for
     * @return The skeleton for that entity
     */
    public abstract S createSkeleton(P parent);

    /**
     * Creates an animator for the specified entity and skeleton.
     *
     * @param parent   The entity to create the animator for
     * @param skeleton The skeleton of the entity
     * @return The animator for that entity
     */
    public abstract Animator<P, S> createAnimator(P parent, S skeleton);

    @Override
    public void render(P parent, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        NecromancerRenderer renderer = NecromancerRenderDispatcher.getRenderer();
        this.render(parent, renderer, VeilRenderBridge.create(poseStack), packedLight, partialTick);
        super.render(parent, entityYaw, partialTick, poseStack, renderer, packedLight);
    }

    /**
     * Renders the necromancer entity.
     *
     * @param parent       The entity to render
     * @param renderer     The renderer instance
     * @param matrixStack  The current transform
     * @param packedLight  The packed lightmap coordinates
     * @param partialTicks The percentage from last tick to this tick
     */
    public void render(P parent, NecromancerRenderer renderer, MatrixStack matrixStack, int packedLight, float partialTicks) {
        S skeleton = parent.getSkeleton();
        if (skeleton == null) {
            return;
        }

        matrixStack.matrixPush();
        matrixStack.applyScale(0.0625F);
        for (NecromancerEntityRenderLayer<P, S> layer : this.layers) {
            layer.render(parent, skeleton, renderer, matrixStack, packedLight, partialTicks);
        }
        matrixStack.matrixPop();
    }

    @Override
    public ResourceLocation getTextureLocation(P entity) {
        throw new UnsupportedOperationException();
    }
}
