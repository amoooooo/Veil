package foundry.veil.api.client.necromancer.render;

import foundry.veil.api.client.necromancer.Skeleton;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public interface NecromancerRenderer extends MultiBufferSource {

    void setUv1(int u, int v);

    void setUv2(int u, int v);

    void setColor(float r, float g, float b, float a);

    void setColor(int color);

    default void setLight(int packedLight) {
        this.setUv2(packedLight & 65535, packedLight >> 16 & 65535);
    }

    default void setOverlay(int packedOverlay) {
        this.setUv1(packedOverlay & 65535, packedOverlay >> 16 & 65535);
    }

    /**
     * Fully resets the renderer state to default.
     */
    void reset();

    /**
     * Queues the specified skeleton with the specified skin to be rendered.
     *
     * @param skeleton The skeleton to draw the skin with
     * @param skin     The skin to draw
     */
    void draw(RenderType renderType, Skeleton skeleton, Skin skin, float partialTicks);
}
