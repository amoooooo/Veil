package foundry.veil.api.quasar.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.quasar.emitters.module.render.TrailSettings;
import foundry.veil.impl.quasar.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.function.Function;

public class Trail {

    public enum TilingMode {
        NONE,
        STRETCH,
        REPEAT;

        public static final Codec<TilingMode> CODEC = Codec.STRING.flatXmap(name -> {
            for (TilingMode value : TilingMode.values()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return DataResult.success(value);
                }
            }
            return DataResult.error(() -> "Unknown Tiling Mode");
        }, tilingMode1 -> DataResult.success(tilingMode1.name().toLowerCase(Locale.ROOT)));
    }

    private Vec3[] points;
    private Vec3[] rotations;
    private int color;
    private Function<Float, Float> widthFunction;
    private int length = 100;
    private boolean billboard = false;
    private TilingMode tilingMode = TilingMode.STRETCH;
    private int frequency = 1;
    private float minDistance = 0f;
    private ResourceLocation texture = null;
    private boolean parentRotation = false;
    private int timeout = 0;

    public Trail(TrailSettings settings) {
        this(MathUtil.colorFromVec4f(settings.getTrailColor()), (ageScale) -> settings.getTrailWidthModifier().modify(ageScale, 1));
        this.billboard = settings.getBillboard();
        this.length = settings.getTrailLength();
        this.frequency = settings.getTrailFrequency();
        this.tilingMode = settings.getTilingMode();
        this.texture = settings.getTrailTexture();
        this.parentRotation = settings.getParentRotation();
    }

    public Trail(Vec3[] points, int color, Function<Float, Float> widthFunction) {
        this.points = points;
        this.color = color;
        this.widthFunction = widthFunction;
    }

    public Trail(int color, Function<Float, Float> widthFunction) {
        this(new Vec3[]{Vec3.ZERO}, color, widthFunction);
    }

    public void setParentRotation(boolean parentRotation) {
        this.parentRotation = parentRotation;
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
        if (points.length > this.length) {
            Vec3[] newPoints = new Vec3[this.length];
            System.arraycopy(points, points.length - this.length, newPoints, 0, this.length);
            points = newPoints;
        }
        this.points = points;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setBillboard(boolean billboard) {
        this.billboard = billboard;
    }

    public void setWidthFunction(Function<Float, Float> widthFunction) {
        this.widthFunction = widthFunction;
    }

    public ResourceLocation getTexture() {
        return this.texture;
    }

    public int getLength() {
        return this.length;
    }

    public void pushPoint(Vec3 point) {
        if (this.timeout > Minecraft.getInstance().getWindow().getRefreshRate() * 5 && this.timeout % 3 == 0) {
            //remove the last point in the array
            Vec3[] newPoints = new Vec3[this.points.length - 1];
            System.arraycopy(this.points, 1, newPoints, 0, this.points.length - 1);
            this.points = newPoints;
            return;
        }
        if (this.points.length == 0) {
            this.points = new Vec3[]{point};
            return;
        }
        if (this.points[this.points.length - 1].distanceTo(point) < this.minDistance) {
            this.timeout++;
            return;
        }
        // test if point is same as last point
        if (this.points[this.points.length - 1].equals(point)) {
            this.timeout++;
            return;
        }
        // add point to end of array and remove first point if array is longer than length
        if (this.points[0] == Vec3.ZERO) {
            this.points[0] = point;
            return;
        }
        this.timeout = 0;
        Vec3[] newPoints = new Vec3[this.points.length + 1];
        System.arraycopy(this.points, 0, newPoints, 0, this.points.length);
        newPoints[this.points.length] = point;
        if (newPoints.length > this.length) {
            Vec3[] newPoints2 = new Vec3[this.length];
            System.arraycopy(newPoints, 1, newPoints2, 0, this.length);
            newPoints = newPoints2;
        }
        this.points = newPoints;
    }

    public void pushRotatedPoint(Vec3 point, Vec3 rotation) {
        if (this.timeout > Minecraft.getInstance().getWindow().getRefreshRate() * 5 && this.timeout % 5 == 0 && this.points.length > 0) {
            //remove the last point in the array
            Vec3[] newPoints = new Vec3[this.points.length - 1];
            System.arraycopy(this.points, 1, newPoints, 0, this.points.length - 1);
            this.points = newPoints;
            Vec3[] newRotations = new Vec3[this.rotations.length - 1];
            System.arraycopy(this.rotations, 1, newRotations, 0, this.rotations.length - 1);
            this.rotations = newRotations;
            return;
        }
        if (this.points.length == 0) {
            this.points = new Vec3[]{point};
            this.rotations = new Vec3[]{rotation};
            return;
        }
        if (this.points[0] == Vec3.ZERO) {
            this.points[0] = point;
            this.rotations = new Vec3[]{rotation};
            return;
        }
        if (this.points[this.points.length - 1].distanceTo(point) < this.minDistance) {
            this.timeout++;
            return;
        }
        // test if point is same as last point
        if (this.points.length > 0 && this.points[this.points.length - 1].equals(point)) {
            this.timeout++;
            return;
        }
        if (this.rotations == null) {
            this.rotations = new Vec3[]{rotation};
        }
        // add point to end of array and remove first point if array is longer than length
        Vec3[] newPoints = new Vec3[this.points.length + 1];
        Vec3[] newRotations = new Vec3[this.points.length + 1];
        System.arraycopy(this.points, 0, newPoints, 0, this.points.length);
        System.arraycopy(this.rotations, 0, newRotations, 0, this.rotations.length);
        newPoints[this.points.length] = point;
        newRotations[this.rotations.length] = rotation;
        if (newPoints.length > this.length) {
            Vec3[] newPoints2 = new Vec3[this.length];
            Vec3[] newRotations2 = new Vec3[this.length];
            System.arraycopy(newPoints, 1, newPoints2, 0, this.length);
            System.arraycopy(newRotations, 1, newRotations2, 0, this.length);
            newPoints = newPoints2;
            newRotations = newRotations2;
        }
        this.points = newPoints;
        this.rotations = newRotations;
    }

    public void render(MatrixStack stack, VertexConsumer consumer, int light) {
        Vector3f[][] corners = new Vector3f[this.points.length][2];
        for (int i = 0; i < this.points.length; i++) {
            if (i % this.frequency != 0) {
                continue;
            }
            float width = this.widthFunction.apply((float) i / (this.points.length - 1));
            Vector3f topOffset = new Vector3f(0, (width / 2f), 0);
            Vector3f bottomOffset = new Vector3f(0, -(width / 2f), 0);
            if (this.billboard) {
                Vec3 a = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(this.points[i]).normalize();
                Vector3f cameraDirection = new Vector3f((float) a.x, (float) a.y, (float) a.z);
                Vec3 b = this.points[Math.min(i + this.frequency, this.points.length - 1)].subtract(this.points[i]).normalize();
                Vector3f dirToNextPoint = new Vector3f((float) b.x(), (float) b.y(), (float) b.z());
                Vector3f axis = new Vector3f(cameraDirection);
                // invert the axis
                axis.mul(-1);
                axis.cross(dirToNextPoint);
                topOffset = new Vector3f(axis);
                topOffset.mul(width / 2f);
                bottomOffset = new Vector3f(axis);
                bottomOffset.mul(-width / 2f);
            } else if (this.rotations[i] != null && this.parentRotation) {
                Vec3 a = this.rotations[Math.min(i + this.frequency, this.rotations.length - 1)];
                Vector3f cameraDirection = new Vector3f((float) a.x, (float) a.y, (float) a.z);
                Vec3 b = this.points[Math.min(i + this.frequency, this.points.length - 1)].subtract(this.points[i]).normalize();
                Vector3f dirToNextPoint = new Vector3f((float) b.x(), (float) b.y(), (float) b.z());
                Vector3f axis = new Vector3f(cameraDirection);
                // invert the axis
                axis.mul(-1);
                axis.cross(dirToNextPoint);
                topOffset = new Vector3f(axis);
                topOffset.mul(width / 2f);
                bottomOffset = new Vector3f(axis);
                bottomOffset.mul(-width / 2f);
            }
            topOffset.add((float) this.points[i].x, (float) this.points[i].y, (float) this.points[i].z);
            bottomOffset.add((float) this.points[i].x, (float) this.points[i].y, (float) this.points[i].z);
            corners[i / this.frequency][0] = topOffset;
            corners[i / this.frequency][1] = bottomOffset;
        }
        this.renderPoints(stack, consumer, light, corners, this.color);
    }

    private void renderPoints(MatrixStack stack, VertexConsumer consumer, int light, Vector3f[][] corners, int color) {
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        int a = color >> 24 & 255;
        for (int i = 0; i < corners.length; i++) {
            Vector3f top = corners[i][0];
            Vector3f bottom = corners[i][1];
//            Vector3f nextTop = corners[i + 1][0];
//            Vector3f nextBottom = corners[i + 1][1];
            if (top == null || bottom == null) {
                continue;
            }
            float u = 0;
            if (this.tilingMode == TilingMode.STRETCH) {
                u = (float) i / (corners.length - 1);
            }
            Matrix4f matrix4f = stack.position();
            consumer.addVertex(matrix4f, bottom.x(), bottom.y(), bottom.z()).setColor(r, g, b, a).setUv(u, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
            consumer.addVertex(matrix4f, top.x(), top.y(), top.z()).setColor(r, g, b, a).setUv(u, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
//            consumer.vertex(matrix4f, nextTop.x(), nextTop.y(), nextTop.z()).color(r, g, b, a).uv(u1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
//            consumer.vertex(matrix4f, nextBottom.x(), nextBottom.y(), nextBottom.z()).color(r, g, b, a).uv(u1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
        }
    }
}
