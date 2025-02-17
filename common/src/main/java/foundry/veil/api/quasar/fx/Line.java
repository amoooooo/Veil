package foundry.veil.api.quasar.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Line {
    public enum TilingMode {
        NONE,
        STRETCH,
        REPEAT
    }

    public enum CurveMode {
        NONE((points, freq) -> points),
        BEZIER((points, freq) -> {
            // calculate bezier curve points and add them to the points array.
            // the number of curve points between each original point should be the frequency divided by amount of input points
            Vec3[] curvePoints = new Vec3[(points.length - 2) * freq];
            for (int i = 0; i < points.length - 1; i++) {
                Vec3 p0 = points[i];
                Vec3 p1 = points[i + 1];
                Vec3 p2 = points[Math.min(i + 2, points.length - 1)];
                Vec3 p3 = points[Math.min(i + 3, points.length - 1)];
                for (int j = 0; j < freq; j++) {
                    float t = (float) j / freq;
                    float t2 = t * t;
                    float t3 = t2 * t;
                    float x = (float) (0.5f * ((2.0f * p1.x()) +
                            (-p0.x() + p2.x()) * t +
                            (2.0f * p0.x() - 5.0f * p1.x() + 4 * p2.x() - p3.x()) * t2 +
                            (-p0.x() + 3.0f * p1.x() - 3.0f * p2.x() + p3.x()) * t3));
                    float y = (float) (0.5f * ((2.0f * p1.y()) +
                            (-p0.y() + p2.y()) * t +
                            (2.0f * p0.y() - 5.0f * p1.y() + 4 * p2.y() - p3.y()) * t2 +
                            (-p0.y() + 3.0f * p1.y() - 3.0f * p2.y() + p3.y()) * t3));
                    float z = (float) (0.5f * ((2.0f * p1.z()) +
                            (-p0.z() + p2.z()) * t +
                            (2.0f * p0.z() - 5.0f * p1.z() + 4 * p2.z() - p3.z()) * t2 +
                            (-p0.z() + 3.0f * p1.z() - 3.0f * p2.z() + p3.z()) * t3));
                    curvePoints[i * freq + j] = new Vec3(x, y, z);
                }
            }
            return curvePoints;
        }),
        CATMULL_ROM((points, freq) -> {
            // calculate catmull-rom curve points and add them to the points array.
            // the number of curve points between each original point should be the frequency divided by amount of input points
            Vec3[] curvePoints = new Vec3[points.length * freq];
            for (int i = 0; i < points.length - 1; i++) {
                Vec3 p0 = points[Math.max(i - 1, 0)];
                Vec3 p1 = points[i];
                Vec3 p2 = points[Math.min(i + 1, points.length - 1)];
                Vec3 p3 = points[Math.min(i + 2, points.length - 1)];
                for (int j = 0; j < freq; j++) {
                    float t = (float) j / freq;
                    float t2 = t * t;
                    float t3 = t2 * t;
                    float x = (float) (0.5f * ((2.0f * p1.x()) +
                            (-p0.x() + p2.x()) * t +
                            (2.0f * p0.x() - 5.0f * p1.x() + 4 * p2.x() - p3.x()) * t2 +
                            (-p0.x() + 3.0f * p1.x() - 3.0f * p2.x() + p3.x()) * t3));
                    float y = (float) (0.5f * ((2.0f * p1.y()) +
                            (-p0.y() + p2.y()) * t +
                            (2.0f * p0.y() - 5.0f * p1.y() + 4 * p2.y() - p3.y()) * t2 +
                            (-p0.y() + 3.0f * p1.y() - 3.0f * p2.y() + p3.y()) * t3));
                    float z = (float) (0.5f * ((2.0f * p1.z()) +
                            (-p0.z() + p2.z()) * t +
                            (2.0f * p0.z() - 5.0f * p1.z() + 4 * p2.z() - p3.z()) * t2 +
                            (-p0.z() + 3.0f * p1.z() - 3.0f * p2.z() + p3.z()) * t3));
                    curvePoints[i * freq + j] = new Vec3(x, y, z);
                }
            }
            return curvePoints;
        });

        final BiFunction<Vec3[], Integer, Vec3[]> curveFunction;

        CurveMode(BiFunction<Vec3[], Integer, Vec3[]> curveFunction) {
            this.curveFunction = curveFunction;
        }
    }

    private Vec3[] points;
    private int color;
    private Function<Float, Float> widthFunction;
    private int length = 100;
    private boolean billboard = true;
    private TilingMode tilingMode = TilingMode.STRETCH;
    private int frequency = 1;
    private float minDistance = 0f;
    private ResourceLocation texture = null;
    private CurveMode curveMode = CurveMode.NONE;

    public Line(Vec3[] points, int color, Function<Float, Float> widthFunction) {
        this.points = points;
        this.color = color;
        this.widthFunction = widthFunction;
    }

    public Line(int color, Function<Float, Float> widthFunction) {
        this(new Vec3[]{Vec3.ZERO}, color, widthFunction);
    }

    public void setCurveMode(CurveMode curveMode) {
        this.curveMode = curveMode;
    }

    public void setTilingMode(TilingMode tilingMode) {
        this.tilingMode = tilingMode;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public void setMinDistance(float minDistance) {
        this.minDistance = minDistance;
    }

    public void setPoints(Vec3[] points) {
        if (points.length > length) {
            Vec3[] newPoints = new Vec3[length];
            System.arraycopy(points, points.length - length, newPoints, 0, length);
            points = newPoints;
        }
        this.points = points;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setWidthFunction(Function<Float, Float> widthFunction) {
        this.widthFunction = widthFunction;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setBillboard(boolean billboard) {
        this.billboard = billboard;
    }

    public Vec3[] getPoints() {
        return points;
    }

    public int getColor() {
        return color;
    }

    public Function<Float, Float> getWidthFunction() {
        return widthFunction;
    }

    public int getLength() {
        return length;
    }

    public boolean getBillboard() {
        return billboard;
    }

    public TilingMode getTilingMode() {
        return tilingMode;
    }

    public int getFrequency() {
        return frequency;
    }

    public float getMinDistance() {
        return minDistance;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public CurveMode getCurveMode() {
        return curveMode;
    }

    public void addPoint(Vec3 point) {
        Vec3[] newPoints = new Vec3[points.length + 1];
        System.arraycopy(points, 0, newPoints, 0, points.length);
        newPoints[points.length] = point;
        points = newPoints;
    }

    public void removePoint(int index) {
        Vec3[] newPoints = new Vec3[points.length - 1];
        System.arraycopy(points, 0, newPoints, 0, index);
        System.arraycopy(points, index + 1, newPoints, index, points.length - index - 1);
        points = newPoints;
    }

    public Vec3[] setupCurvePoints() {
        // calculate curve points and add them to the points array
        return curveMode.curveFunction.apply(points, frequency);
    }

    public void render(PoseStack stack, VertexConsumer consumer, int light) {
        stack.pushPose();
        RenderSystem.disableCull();
        Vec3[] curvePoints = setupCurvePoints();
        Vector3f[][] corners = new Vector3f[curvePoints.length][2];
        for (int i = 0; i < curvePoints.length; i++) {
            float width = widthFunction.apply((float) i / (curvePoints.length - 1));
            Vector3f topOffset = new Vector3f(0, (width / 2f), 0);
            Vector3f bottomOffset = new Vector3f(0, -(width / 2f), 0);
            if (billboard) {
//                Vector3f cameraDirection = new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(curvePoints[i]).normalize());
//                Vector3f dirToNextPoint = new Vector3f(curvePoints[Math.min(i + frequency, curvePoints.length - 1)].subtract(curvePoints[i]).normalize());
//                Vector3f axis = cameraDirection.copy();
//                // invert the axis
//                axis.mul(-1);
//                axis.cross(dirToNextPoint);
//                topOffset = axis.copy();
//                topOffset.mul(width/2f);
//                bottomOffset = axis.copy();
//                bottomOffset.mul(-width/2f);
            }
            topOffset.add((float) curvePoints[i].x, (float) curvePoints[i].y, (float) curvePoints[i].z);
            bottomOffset.add((float) curvePoints[i].x, (float) curvePoints[i].y, (float) curvePoints[i].z);
            corners[i / frequency][0] = topOffset;
            corners[i / frequency][1] = bottomOffset;
        }
        renderPoints(stack, consumer, light, corners, color);
        RenderSystem.enableCull();
        stack.popPose();
    }

    private void renderPoints(PoseStack stack, VertexConsumer consumer, int light, Vector3f[][] corners, int color) {
        stack.pushPose();
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;
        float a = (color >> 24 & 255) / 255f;
        for (int i = 0; i < corners.length - 1; i++) {
            Vector3f top = corners[i][0];
            Vector3f bottom = corners[i][1];
            Vector3f nextTop = corners[i + 1][0];
            Vector3f nextBottom = corners[i + 1][1];
            if (nextTop == null) {
                nextTop = top;
            }
            if (nextBottom == null) {
                nextBottom = bottom;
            }
            if (top == null || bottom == null) {
                continue;
            }
            float u = 0;
            float u1 = 1;
            if (tilingMode == TilingMode.STRETCH) {
                u = (float) i / (corners.length - 1);
                u1 = (float) (i + 1) / (corners.length - 1);
            }
            consumer.addVertex(stack.last().pose(), bottom.x(), bottom.y(), bottom.z()).setColor(r, g, b, a).setUv(u, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
            consumer.addVertex(stack.last().pose(), top.x(), top.y(), top.z()).setColor(r, g, b, a).setUv(u, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
            consumer.addVertex(stack.last().pose(), nextTop.x(), nextTop.y(), nextTop.z()).setColor(r, g, b, a).setUv(u1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
            consumer.addVertex(stack.last().pose(), nextBottom.x(), nextBottom.y(), nextBottom.z()).setColor(r, g, b, a).setUv(u1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        }
        stack.popPose();
    }

    public enum RenderMode {
        FLAT,
        CUBOID;
    }
}
