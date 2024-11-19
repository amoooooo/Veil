package foundry.veil.api.quasar.emitters.shape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.RandomSource;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Disc implements EmitterShape {

    @Override
    public Vector3d getPoint(RandomSource randomSource, Vector3fc dimensions, Vector3fc rotation, Vector3dc position, boolean fromSurface) {
        double x = randomSource.nextGaussian();
        double y = 0.0000001;
        double z = randomSource.nextGaussian();
        Vector3d normal = new Vector3d(x, y, z).normalize();
        Vector3fc dim = dimensions;
        if (!fromSurface) {
            normal.mul(randomSource.nextDouble()).normalize();
            dim = dimensions.mul(
                    randomSource.nextFloat(),
                    randomSource.nextFloat(),
                    randomSource.nextFloat(),
                    new Vector3f()
            );
        }
        Vector3d pos = normal.mul(dim);
        pos = pos.rotateX((float) Math.toRadians(rotation.x())).rotateY((float) Math.toRadians(rotation.y())).rotateZ((float) Math.toRadians(rotation.z()));
        return pos.add(position);
    }

    @Override
    public void renderShape(PoseStack stack, VertexConsumer consumer, Vector3fc dimensions, Vector3fc rotation) {
        float radius = dimensions.x();
        float angle = 0;
        float angleStep = 360f / 32f;
        float x = (float) 0;
        float y = (float) 0;
        float z = (float) 0;
        for (int i = 0; i < 32; i++) {
            float x1 = (float) (x + Math.cos(Math.toRadians(angle)) * radius);
            float z1 = (float) (z + Math.sin(Math.toRadians(angle)) * radius);
            float x2 = (float) (x + Math.cos(Math.toRadians(angle + angleStep)) * radius);
            float z2 = (float) (z + Math.sin(Math.toRadians(angle + angleStep)) * radius);
            consumer.addVertex(stack.last().pose(), x1, y, z1).setColor(0.15f, 0.15f, 1, 1).setNormal(0, 1, 0);
            angle += angleStep;
        }
    }
}
