package foundry.veil.api.client.necromancer.render;

import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Renders a single skin with a render type for a necromancer entity.
 *
 * @param <T> The type of entity to render
 * @param <M> The skeleton to render
 */
public abstract class NecromancerSkinEntityRenderLayer<T extends Entity & SkeletonParent<T, M>, M extends Skeleton> extends NecromancerEntityRenderLayer<T, M> {

    public NecromancerSkinEntityRenderLayer(NecromancerEntityRenderer<T, M> renderer) {
        super(renderer);
    }

    /**
     * Retrieves the render type to use for the specified entity.
     *
     * @param parent The entity to get the render type for
     * @return The render type or <code>null</code> to skip rendering
     */
    public abstract @Nullable RenderType getRenderType(T parent);

    /**
     * Retrieves the skin to use for the specified entity.
     *
     * @param parent The entity to get the skin for
     * @return The skin or <code>null</code> to skip rendering
     */
    public abstract @Nullable Skin getSkin(T parent);

    @Override
    public void render(T parent, M skeleton, NecromancerRenderer renderer, MatrixStack matrixStack, int packedLight, float partialTicks) {
        RenderType renderType = this.getRenderType(parent);
        if (renderType != null) {
            Skin skin = this.getSkin(parent);
            if (skin != null) {
                this.renderSkin(parent, skeleton, skin, renderType, renderer, matrixStack, packedLight, partialTicks);
            }
        }
    }

    /**
     * Renders the skin on the specified entity.
     *
     * @param parent       The entity to draw the skin for
     * @param skeleton     The skeleton of the entity to base a pose on
     * @param skin         The skin to draw
     * @param renderType   The render type to use
     * @param renderer     The renderer instance
     * @param matrixStack  The current transform
     * @param packedLight  The packed lightmap coordinates
     * @param partialTicks The percentage from last tick to this tick
     */
    protected void renderSkin(T parent, M skeleton, Skin skin, RenderType renderType, NecromancerRenderer renderer, MatrixStack matrixStack, int packedLight, float partialTicks) {
        renderer.setTransform(matrixStack.position());
        renderer.setLight(packedLight);
        renderer.draw(renderType, skeleton, skin, partialTicks);
        renderer.reset();
    }
}
