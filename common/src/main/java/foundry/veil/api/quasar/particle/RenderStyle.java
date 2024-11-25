package foundry.veil.api.quasar.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.quasar.data.QuasarParticleData;
import foundry.veil.api.quasar.registry.RenderStyleRegistry;
import foundry.veil.api.util.CodecUtil;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public interface RenderStyle {
    Codec<RenderStyle> CODEC = CodecUtil.registryOrLegacyCodec(RenderStyleRegistry.REGISTRY);

    void render(MatrixStack matrixStack, QuasarParticle particle, RenderData renderData, Vector3fc renderOffset, VertexConsumer builder, double ageModifier, float partialTicks);

    final class Cube implements RenderStyle {
        private static final Vector3fc[] CUBE_POSITIONS = {
                // TOP
                new Vector3f(1, 1, -1), new Vector3f(1, 1, 1), new Vector3f(-1, 1, 1), new Vector3f(-1, 1, -1),

                // BOTTOM
                new Vector3f(-1, -1, -1), new Vector3f(-1, -1, 1), new Vector3f(1, -1, 1), new Vector3f(1, -1, -1),

                // FRONT
                new Vector3f(-1, -1, 1), new Vector3f(-1, 1, 1), new Vector3f(1, 1, 1), new Vector3f(1, -1, 1),

                // BACK
                new Vector3f(1, -1, -1), new Vector3f(1, 1, -1), new Vector3f(-1, 1, -1), new Vector3f(-1, -1, -1),

                // LEFT
                new Vector3f(-1, -1, -1), new Vector3f(-1, 1, -1), new Vector3f(-1, 1, 1), new Vector3f(-1, -1, 1),

                // RIGHT
                new Vector3f(1, -1, 1), new Vector3f(1, 1, 1), new Vector3f(1, 1, -1), new Vector3f(1, -1, -1)};
        private static final float[] CUBE_UVS = {0, 0, 0, 1, 1, 1, 1, 0};
        private static final float[] CUBE_NORMALS = {0, 1, 0, 0, -1, 0, 0, 0, 1, 0, 0, -1, -1, 0, 0, 1, 0, 0};
        private static final Vector3f POS = new Vector3f();

        @Override
        public void render(MatrixStack matrixStack, QuasarParticle particle, RenderData renderData, Vector3fc renderOffset, VertexConsumer builder, double ageModifier, float partialTicks) {
            Matrix4f matrix4f = matrixStack.position();
            Vector3fc rotation = renderData.getRenderRotation();
            SpriteData spriteData = renderData.getSpriteData();

            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 4; j++) {
                    POS.set(CUBE_POSITIONS[i * 4 + j]);
                    QuasarParticleData data = particle.getData();
                    if (POS.z < 0 && data.velocityStretchFactor() != 0.0f) {
                        POS.z *= 1 + data.velocityStretchFactor();
                    }
                    POS.rotateX(rotation.x())
                            .rotateY(rotation.y())
                            .rotateZ(rotation.z())
                            .mul((float) (renderData.getRenderRadius() * ageModifier))
                            .add(renderOffset);

                    float u = CUBE_UVS[j * 2];
                    float v = CUBE_UVS[j * 2 + 1];

                    if (spriteData != null) {
                        u = spriteData.u(renderData.getRenderAge(), renderData.getAgePercent(), u);
                        v = spriteData.v(renderData.getRenderAge(), renderData.getAgePercent(), v);
                    }

                    builder.addVertex(matrix4f, POS.x, POS.y, POS.z);
                    builder.setColor(renderData.getRed(), renderData.getGreen(), renderData.getBlue(), renderData.getAlpha());
                    builder.setUv(u, v);
                    builder.setLight(renderData.getLightColor());
                    builder.setNormal(CUBE_NORMALS[i * 3], CUBE_NORMALS[i * 3 + 1], CUBE_NORMALS[i * 3 + 2]);
                }
            }
        }
    }

    final class Billboard implements RenderStyle {
        private static final Vector3fc[] PLANE_POSITIONS = {
                // plane from -1 to 1 on Y axis and -1 to 1 on X axis
                new Vector3f(-1, -1, 0), new Vector3f(-1, 1, 0), new Vector3f(1, 1, 0), new Vector3f(1, -1, 0)
        };
        private static final float[] PLANE_UVS = {0, 0, 0, 1, 1, 1, 1, 0};
        private static final Vector3f POS = new Vector3f();
        private static final Vector3f NORMAL = new Vector3f();

        @Override
        public void render(MatrixStack matrixStack, QuasarParticle particle, RenderData renderData, Vector3fc renderOffset, VertexConsumer builder, double ageModifier, float partialTicks) {
            Matrix4f matrix4f = matrixStack.position();
            Vector3fc rotation = renderData.getRenderRotation();

            Quaternionf faceCameraRotation = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
            SpriteData spriteData = renderData.getSpriteData();

            int red = (int) (renderData.getRed() * 255.0F) & 0xFF;
            int green = (int) (renderData.getGreen() * 255.0F) & 0xFF;
            int blue = (int) (renderData.getBlue() * 255.0F) & 0xFF;
            int alpha = (int) (renderData.getAlpha() * 255.0F) & 0xFF;

            NORMAL.set(0, 0, -1);
            if (particle.getData().faceVelocity()) {
                NORMAL.rotateX(rotation.x())
                        .rotateY(rotation.y())
                        .rotateZ(rotation.z());
            }

            // turn quat into pitch and yaw
            for (int j = 0; j < 4; j++) {
                POS.set(PLANE_POSITIONS[j]);
                if (particle.getData().velocityStretchFactor() > 0f) {
                    POS.set(POS.x * (1 + particle.getData().velocityStretchFactor()), POS.y, POS.z);
                }
                if (particle.getData().faceVelocity()) {
                    POS.rotateX(rotation.x())
                            .rotateY(rotation.y())
                            .rotateZ(rotation.z());
                }
//                vec = vec.xRot(lerpedPitch).yRot(lerpedYaw).zRot(lerpedRoll);
                faceCameraRotation.transform(POS).mul((float) (renderData.getRenderRadius() * ageModifier)).add(renderOffset);

                float u = PLANE_UVS[j * 2];
                float v = PLANE_UVS[j * 2 + 1];
                if (spriteData != null) {
                    u = spriteData.u(renderData.getRenderAge(), renderData.getAgePercent(), u);
                    v = spriteData.v(renderData.getRenderAge(), renderData.getAgePercent(), v);
                }
//                    if (particle.sprite != null) {
//                        u1 = u;
//                        v1 = v;
//                    }
                builder.addVertex(matrix4f, POS.x, POS.y, POS.z);
                builder.setColor(red, green, blue, alpha);
                builder.setUv(u, v);
                builder.setLight(renderData.getLightColor());
                builder.setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
            }
        }
    }
}
