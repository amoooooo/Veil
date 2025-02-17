package foundry.veil.api.quasar.emitters.shape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.*;

import java.lang.Math;

public class Sphere implements EmitterShape {

    @Override
    public Vector3d getPoint(RandomSource randomSource, Vector3fc dimensions, Vector3fc rotation, Vector3dc position, boolean fromSurface) {
        double x = randomSource.nextDouble() * 2 - 1;
        double y = randomSource.nextDouble() * 2 - 1;
        double z = randomSource.nextDouble() * 2 - 1;
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
        Matrix4f matrix4f = stack.last().pose();
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                Vector3f v1 = parametricSphere((float) Math.toRadians(i * 11.25f), (float) Math.toRadians(j * 11.25f), radius);
                Vector3f v2 = parametricSphere((float) Math.toRadians((i + 1) * 11.25f), (float) Math.toRadians(j * 11.25f), radius);
                Vector3f v3 = parametricSphere((float) Math.toRadians(i * 11.25f), (float) Math.toRadians((j + 1) * 11.25f), radius);
                Vector3f v4 = parametricSphere((float) Math.toRadians((i + 1) * 11.25f), (float) Math.toRadians((j + 1) * 11.25f), radius);
                consumer.addVertex(matrix4f, v1.x(), v1.y(), v1.z()).setColor(0.15f, 0.15f, 1, 1).setNormal(0, 1, 0);
                consumer.addVertex(matrix4f, v2.x(), v2.y(), v2.z()).setColor(0.15f, 0.15f, 1, 1).setNormal(0, 1, 0);
                consumer.addVertex(matrix4f, v3.x(), v3.y(), v3.z()).setColor(0.15f, 0.15f, 1, 1).setNormal(0, 1, 0);
                consumer.addVertex(matrix4f, v4.x(), v4.y(), v4.z()).setColor(0.15f, 0.15f, 1, 1).setNormal(0, 1, 0);
            }
        }
    }

    public static Vector3f parametricSphere(float u, float v, float r) {
        return new Vector3f(Mth.cos(u) * Mth.sin(v) * r, Mth.cos(v) * r, Mth.sin(u) * Mth.sin(v) * r);
    }
}
