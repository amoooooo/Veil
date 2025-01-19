package foundry.veil.api.client.necromancer.render;

import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.world.entity.Entity;

/**
 * A single render layer of an entity.
 *
 * @param <T> The type of entity to render
 * @param <M> The skeleton to render
 */
public abstract class NecromancerEntityRenderLayer<T extends Entity & SkeletonParent<T, M>, M extends Skeleton> {

    protected final NecromancerEntityRenderer<T, M> renderer;

    public NecromancerEntityRenderLayer(NecromancerEntityRenderer<T, M> renderer) {
        this.renderer = renderer;
    }

    /**
     * Renders a feature for the specified entity and skeleton.
     *
     * @param parent       The entity to draw
     * @param skeleton     The skeleton of the entity to base a pose on
     * @param renderer     The renderer instance
     * @param matrixStack  The current transform
     * @param packedLight  The packed lightmap coordinates
     * @param partialTicks The percentage from last tick to this tick
     */
    public abstract void render(T parent, M skeleton, NecromancerRenderer renderer, MatrixStack matrixStack, int packedLight, float partialTicks);
}
