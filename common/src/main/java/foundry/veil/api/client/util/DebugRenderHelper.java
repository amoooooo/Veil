package foundry.veil.api.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// bunch of static functions for rendering spheres etc
// all pretty self-explanatory
public class DebugRenderHelper {
    public static void renderCube(PoseStack pPoseStack, VertexConsumer pConsumer, float pRed, float pGreen, float pBlue, float pAlpha) {
        renderBox(pPoseStack, pConsumer, 0, 0, 0, 1, 1, 1, pRed, pGreen, pBlue, pAlpha);
    }

    public static void renderBox(PoseStack pPoseStack, VertexConsumer pConsumer, double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ, float pRed, float pGreen, float pBlue, float pAlpha) {
        PoseStack.Pose pose = pPoseStack.last();
        float iX = (float) pMinX;
        float iY = (float) pMinY;
        float iZ = (float) pMinZ;
        float aX = (float) pMaxX;
        float aY = (float) pMaxY;
        float aZ = (float) pMaxZ;
        pConsumer.addVertex(pose, iX, iY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(pose, aX, iY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(pose, iX, iY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(pose, iX, aY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(pose, iX, iY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
        pConsumer.addVertex(pose, iX, iY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
        pConsumer.addVertex(pose, aX, iY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(pose, aX, aY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(pose, aX, aY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, -1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(pose, iX, aY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, -1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(pose, iX, aY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
        pConsumer.addVertex(pose, iX, aY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
        pConsumer.addVertex(pose, iX, aY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, -1.0F, 0.0F);
        pConsumer.addVertex(pose, iX, iY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, -1.0F, 0.0F);
        pConsumer.addVertex(pose, iX, iY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(pose, aX, iY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(pose, aX, iY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 0.0F, -1.0F);
        pConsumer.addVertex(pose, aX, iY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 0.0F, -1.0F);
        pConsumer.addVertex(pose, iX, aY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(pose, aX, aY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(pose, aX, iY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(pose, aX, aY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(pose, aX, aY, iZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
        pConsumer.addVertex(pose, aX, aY, aZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    public static void renderBox(PoseStack pPoseStack, VertexConsumer pConsumer, AABB box, float pRed, float pGreen, float pBlue, float pAlpha) {
        renderBox(pPoseStack, pConsumer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, pRed, pGreen, pBlue, pAlpha);
    }

    public static void renderSphere(PoseStack pPoseStack, VertexConsumer pConsumer, int resolution, float radius, double x, double y, double z, float pRed, float pGreen, float pBlue, float pAlpha) {
        pPoseStack.pushPose();
        pPoseStack.translate(x, y, z);

        for (int i = 0; i < 3; i++) {
            pPoseStack.pushPose();
            switch (i) {
                case 1:
                    pPoseStack.mulPose(Axis.YP.rotationDegrees(90));
                    break;
                case 2:
                    pPoseStack.mulPose(Axis.XP.rotationDegrees(90));
                    break;
            }
            PoseStack.Pose pose = pPoseStack.last();
            pPoseStack.popPose();
            for (int segment = 0; segment < resolution; segment++) {
                float angle1 = (segment / (float) resolution) * Mth.TWO_PI;
                float angle2 = ((segment + 1) / (float) resolution) * Mth.TWO_PI;
                float s1 = Mth.sin(angle1) * radius;
                float c1 = Mth.cos(angle1) * radius;
                float s2 = Mth.sin(angle2) * radius;
                float c2 = Mth.cos(angle2) * radius;

                Vector3f normal = new Vector3f(s1, 0, c1);
                normal.sub(new Vector3f(s2, 0, c2));
                normal.normalize();

                renderLine(pose, pConsumer, s1, c1, 0, s2, c2, 0, pRed, pGreen, pBlue, pAlpha);
            }
        }

        pPoseStack.popPose();
    }

    public static void renderCircle(PoseStack pPoseStack, VertexConsumer pConsumer, int resolution, float radius, double x, double y, double z, float pRed, float pGreen, float pBlue, float pAlpha) {
        Minecraft mc = Minecraft.getInstance();
        Camera pRenderInfo = mc.gameRenderer.getMainCamera();

        Quaternionf rotation = pRenderInfo.rotation();
        pPoseStack.pushPose();
        pPoseStack.translate(x, y, z);
        pPoseStack.mulPose(rotation);

        PoseStack.Pose pose = pPoseStack.last();
        for (int segment = 0; segment < resolution; segment++) {
            float angle1 = (segment / (float) resolution) * Mth.TWO_PI;
            float angle2 = ((segment + 1) / (float) resolution) * Mth.TWO_PI;
            float s1 = Mth.sin(angle1) * radius;
            float c1 = Mth.cos(angle1) * radius;
            float s2 = Mth.sin(angle2) * radius;
            float c2 = Mth.cos(angle2) * radius;

            Vector3f normal = new Vector3f(s1, 0, c1);
            normal.sub(new Vector3f(s2, 0, c2));
            normal.normalize();

            renderLine(pose, pConsumer, s1, c1, 0, s2, c2, 0, pRed, pGreen, pBlue, pAlpha);
        }

        pPoseStack.popPose();
    }

    public static void renderLine(PoseStack pPoseStack, VertexConsumer pConsumer, double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ, float pRed, float pGreen, float pBlue, float pAlpha) {
        PoseStack.Pose pose = pPoseStack.last();
        float minX = (float) pMinX;
        float minY = (float) pMinY;
        float minZ = (float) pMinZ;
        float maxX = (float) pMaxX;
        float maxY = (float) pMaxY;
        float maxZ = (float) pMaxZ;
        Vector3f normal = new Vector3f(minX, minY, minZ);
        normal.sub(new Vector3f(maxX, maxY, maxZ));
        normal.normalize();

        pConsumer.addVertex(pose, minX, minY, minZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, normal.x(), normal.y(), normal.z());
        pConsumer.addVertex(pose, maxX, maxY, maxZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, normal.x(), normal.y(), normal.z());
    }

    public static void renderLine(PoseStack pPoseStack, VertexConsumer pConsumer, double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ, float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2) {
        PoseStack.Pose pose = pPoseStack.last();
        float minX = (float) pMinX;
        float minY = (float) pMinY;
        float minZ = (float) pMinZ;
        float maxX = (float) pMaxX;
        float maxY = (float) pMaxY;
        float maxZ = (float) pMaxZ;
        Vector3f normal = new Vector3f(minX, minY, minZ);
        normal.sub(new Vector3f(maxX, maxY, maxZ));
        normal.normalize();

        pConsumer.addVertex(pose, minX, minY, minZ).setColor(r1, g1, b1, a1).setNormal(pose, normal.x(), normal.y(), normal.z());
        pConsumer.addVertex(pose, maxX, maxY, maxZ).setColor(r2, g2, b2, a2).setNormal(pose, normal.x(), normal.y(), normal.z());
    }

    public static void renderLine(PoseStack.Pose pose, VertexConsumer pConsumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float pRed, float pGreen, float pBlue, float pAlpha) {
        Vector3f normal = new Vector3f(minX, minY, minZ);
        normal.sub(new Vector3f(maxX, maxY, maxZ));
        normal.normalize();

        pConsumer.addVertex(pose, minX, minY, minZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, normal.x(), normal.y(), normal.z());
        pConsumer.addVertex(pose, maxX, maxY, maxZ).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(pose, normal.x(), normal.y(), normal.z());
    }
}
