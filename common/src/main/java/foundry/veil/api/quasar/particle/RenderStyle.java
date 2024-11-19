package foundry.veil.api.quasar.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
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

    void render(PoseStack poseStack, QuasarParticle particle, RenderData renderData, Vector3fc renderOffset, VertexConsumer builder, double ageModifier, float partialTicks);

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

        @Override
        public void render(PoseStack poseStack, QuasarParticle particle, RenderData renderData, Vector3fc renderOffset, VertexConsumer builder, double ageModifier, float partialTicks) {
            Matrix4f matrix4f = poseStack.last().pose();
            Vector3fc rotation = renderData.getRenderRotation();
            Vector3f vec = new Vector3f();
            SpriteData spriteData = renderData.getSpriteData();

            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 4; j++) {
                    vec.set(CUBE_POSITIONS[i * 4 + j]);
                    QuasarParticleData data = particle.getData();
                    if (vec.z < 0 && data.velocityStretchFactor() != 0.0f) {
                        vec.z *= 1 + data.velocityStretchFactor();
                    }
                    vec.rotateX(rotation.x())
                            .rotateY(rotation.y())
                            .rotateZ(rotation.z())
                            .mul((float) (renderData.getRenderRadius() * ageModifier))
                            .add(renderOffset);

                    float u = (int) (j / 2.0F);
                    float v = j % 2;

                    if (spriteData != null) {
                        u = spriteData.u(renderData.getRenderAge(), renderData.getAgePercent(), u);
                        v = spriteData.v(renderData.getRenderAge(), renderData.getAgePercent(), v);
                    }

                    builder.addVertex(matrix4f, vec.x, vec.y, vec.z);
                    builder.setUv(u, v);
                    builder.setColor(renderData.getRed(), renderData.getGreen(), renderData.getBlue(), renderData.getAlpha());
                    builder.setLight(renderData.getLightColor());
                }
            }
        }
    }

    final class Billboard implements RenderStyle {
        private static final Vector3fc[] PLANE_POSITIONS = {
                // plane from -1 to 1 on Y axis and -1 to 1 on X axis
                new Vector3f(1, -1, 0), new Vector3f(-1, -1, 0), new Vector3f(-1, 1, 0), new Vector3f(1, 1, 0)
        };

        @Override
        public void render(PoseStack poseStack, QuasarParticle particle, RenderData renderData, Vector3fc renderOffset, VertexConsumer builder, double ageModifier, float partialTicks) {
            //TODO fix UVs theyre fucked
            Matrix4f matrix4f = poseStack.last().pose();
            Vector3fc rotation = renderData.getRenderRotation();

            Quaternionf faceCameraRotation = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
            SpriteData spriteData = renderData.getSpriteData();

            int red = (int) (renderData.getRed() * 255.0F) & 0xFF;
            int green = (int) (renderData.getGreen() * 255.0F) & 0xFF;
            int blue = (int) (renderData.getBlue() * 255.0F) & 0xFF;
            int alpha = (int) (renderData.getAlpha() * 255.0F) & 0xFF;

            // turn quat into pitch and yaw
            Vector3f vec = new Vector3f();
            for (int j = 0; j < 4; j++) {
                vec.set(PLANE_POSITIONS[j]);
                if (particle.getData().velocityStretchFactor() > 0f) {
                    vec.set(vec.x * (1 + particle.getData().velocityStretchFactor()), vec.y, vec.z);
                }
                if (particle.getData().faceVelocity()) {
                    vec.rotateX(rotation.x())
                            .rotateY(rotation.y())
                            .rotateZ(rotation.z());
                }
//                vec = vec.xRot(lerpedPitch).yRot(lerpedYaw).zRot(lerpedRoll);
                faceCameraRotation.transform(vec).mul((float) (renderData.getRenderRadius() * ageModifier)).add(renderOffset);

                float u, v;
                if (j == 0) {
                    u = 0;
                    v = 0;
                } else if (j == 1) {
                    u = 1;
                    v = 0;
                } else if (j == 2) {
                    u = 1;
                    v = 1;
                } else {
                    u = 0;
                    v = 1;
                }
                if (spriteData != null) {
                    u = spriteData.u(renderData.getRenderAge(), renderData.getAgePercent(), u);
                    v = spriteData.v(renderData.getRenderAge(), renderData.getAgePercent(), v);
                }
//                    if (particle.sprite != null) {
//                        u1 = u;
//                        v1 = v;
//                    }
                builder.addVertex(matrix4f, vec.x, vec.y, vec.z);
                builder.setUv(u, v);
                builder.setColor(red, green, blue, alpha);
                builder.setLight(renderData.getLightColor());
            }
        }
    }
}
