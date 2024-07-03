package foundry.veil.api.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderBridge;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Draws any arbitrary view frustum.
 *
 * @author Ocelot
 */
public class DebugFrustumRenderer {

    private static final Vector3f[] POSITIONS = new Vector3f[8];
    private static final Matrix4f MODELVIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();

    static {
        for (int i = 0; i < POSITIONS.length; i++) {
            POSITIONS[i] = new Vector3f();
        }
    }

    private static int getIndex(int x, int y, int z) {
        return (z * 2 + y) * 2 + x;
    }

    private static void putLine(@NotNull VertexConsumer consumer,
                                int x0,
                                int y0,
                                int z0,
                                int x1,
                                int y1,
                                int z1,
                                float red,
                                float green,
                                float blue,
                                float alpha) {
        Vector3fc pos0 = POSITIONS[getIndex(x0, y0, z0)];
        Vector3fc pos1 = POSITIONS[getIndex(x1, y1, z1)];
        Vector3fc norm = pos1.sub(pos0, new Vector3f()).normalize();

        consumer.vertex(pos0.x(), pos0.y(), pos0.z())
                .color(red, green, blue, alpha)
                .normal(norm.x(), norm.y(), norm.z())
                .endVertex();
        consumer.vertex(pos1.x(), pos1.y(), pos1.z())
                .color(red, green, blue, alpha)
                .normal(norm.x(), norm.y(), norm.z())
                .endVertex();
    }

    public static void renderFrustum(@NotNull MultiBufferSource source,
                                     @NotNull PoseStack stack,
                                     @NotNull Matrix4fc pose,
                                     @NotNull Matrix4fc projection,
                                     float red,
                                     float green,
                                     float blue,
                                     float alpha) {
        stack.pushPose();
        Matrix4f modelView = stack.last().pose();
        modelView.mul(pose.invert(MODELVIEW));
        modelView.mul(projection.invert(PROJECTION));

        for (int z = 0; z < 2; z++) {
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    modelView.transformPosition(x * 2 - 1,
                            y * 2 - 1,
                            z * 2 - 1,
                            POSITIONS[getIndex(x, y, z)]);
                }
            }
        }

        VertexConsumer consumer = source.getBuffer(RenderType.lines());

        // Near
        putLine(consumer, 0, 0, 0, 0, 1, 0, red, green, blue, alpha);
        putLine(consumer, 0, 0, 1, 0, 1, 1, red, green, blue, alpha);
        putLine(consumer, 0, 0, 0, 0, 0, 1, red, green, blue, alpha);

        putLine(consumer, 0, 1, 0, 1, 1, 0, red, green, blue, alpha);
        putLine(consumer, 0, 1, 1, 1, 1, 1, red, green, blue, alpha);
        putLine(consumer, 0, 1, 0, 0, 1, 1, red, green, blue, alpha);

        putLine(consumer, 1, 1, 0, 1, 0, 0, red, green, blue, alpha);
        putLine(consumer, 1, 1, 1, 1, 0, 1, red, green, blue, alpha);
        putLine(consumer, 1, 1, 0, 1, 1, 1, red, green, blue, alpha);

        putLine(consumer, 1, 0, 0, 0, 0, 0, red, green, blue, alpha);
        putLine(consumer, 1, 0, 1, 0, 0, 1, red, green, blue, alpha);
        putLine(consumer, 1, 0, 0, 1, 0, 1, red, green, blue, alpha);

        stack.popPose();
    }
}
