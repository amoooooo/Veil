package foundry.veil.api.client.render.light;

import foundry.veil.api.client.editor.EditorAttributeProvider;
import foundry.veil.api.client.registry.LightTypeRegistry;
import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.type.ImDouble;
import imgui.type.ImFloat;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.lang.Math;
import java.nio.ByteBuffer;

public class AreaLight extends Light implements InstancedLight, PositionedLight<AreaLight>, EditorAttributeProvider {

    private static final float MAX_ANGLE_SIZE = (float) (65535 / 2 / Math.PI);

    protected final Vector3d position;
    protected final Quaternionf orientation;
    private final Matrix4d matrix;

    protected final Vector2f size;

    protected float angle;
    protected float distance;

    public AreaLight() {
        this.matrix = new Matrix4d();
        this.position = new Vector3d();
        this.orientation = new Quaternionf();

        this.size = new Vector2f(1.0F, 1.0F);

        this.angle = (float) Math.toRadians(45);
        this.distance = 1.0F;
    }

    @Override
    public void store(ByteBuffer buffer) {
        this.matrix.getFloats(buffer.position(), buffer);
        buffer.position(buffer.position() + Float.BYTES * 16);

        buffer.putFloat(this.color.x() * this.brightness);
        buffer.putFloat(this.color.y() * this.brightness);
        buffer.putFloat(this.color.z() * this.brightness);

        this.size.get(buffer.position(), buffer);
        buffer.position(buffer.position() + Float.BYTES * 2);

        buffer.putShort((short) Mth.clamp((int) (this.angle * MAX_ANGLE_SIZE), 0, 65535));
        buffer.putFloat(this.distance);
    }

    @Override
    public LightTypeRegistry.LightType<?> getType() {
        return LightTypeRegistry.AREA.get();
    }

    @Override
    public Vector3d getPosition() {
        return this.position;
    }

    /**
     * @return The current orientation of the light.
     */
    public Quaternionf getOrientation() {
        return this.orientation;
    }

    /**
     * @return The size of the light's surface
     */
    public Vector2f getSize() {
        return this.size;
    }

    /**
     * @return The maximum angle of the light from the plane's surface.
     */
    public float getAngle() {
        return this.angle;
    }

    /**
     * @return The maximum distance the light can travel
     */
    public float getDistance() {
        return this.distance;
    }

    @Override
    public AreaLight setColor(float red, float green, float blue) {
        return (AreaLight) super.setColor(red, green, blue);
    }

    @Override
    public AreaLight setColor(Vector3fc color) {
        return (AreaLight) super.setColor(color);
    }

    @Override
    public AreaLight setBrightness(float brightness) {
        return (AreaLight) super.setBrightness(brightness);
    }

    @Override
    public AreaLight setPosition(double x, double y, double z) {
        this.position.set(x, y, z);
        this.updateMatrix();
        return this;
    }

    /**
     * Sets the orientation of the light's surface
     *
     * @param orientation The orientation of the light's surface.
     */
    public AreaLight setOrientation(Quaternionfc orientation) {
        this.orientation.set(orientation).normalize();
        this.updateMatrix();
        return this;
    }

    /**
     * Sets the size of the light's surface
     *
     * @param x The length, in blocks, of the light's surface.
     * @param y The width, in blocks, of the light's surface.
     */
    public AreaLight setSize(double x, double y) {
        this.size.set(x, y);
        this.markDirty();
        return this;
    }

    /**
     * Sets the maximum angle the light can influence.
     *
     * @param angle The maximum angle of the light's influence in radians
     */
    public AreaLight setAngle(float angle) {
        this.angle = angle;
        this.markDirty();
        return this;
    }

    /**
     * Sets the maximum distance the light can influence.
     *
     * @param distance The maximum area of influence for the light
     */
    public AreaLight setDistance(float distance) {
        this.distance = distance;
        this.markDirty();
        return this;
    }

    @Override
    public Light setTo(Camera camera) {
        Vec3 pos = camera.getPosition();
        return this.setPosition(pos.x, pos.y, pos.z).setOrientation(new Quaternionf().lookAlong(camera.getLookVector().mul(-1), camera.getUpVector()));
    }

    protected void updateMatrix() {
        Vector3d position = this.getPosition();
        Quaternionf orientation = this.getOrientation();
        this.matrix.rotation(orientation).translate(position);
        this.markDirty();
    }

    @Override
    public AreaLight clone() {
        AreaLight light = new AreaLight();
        light.matrix.set(this.matrix);
        light.size.set(this.size);
        light.angle = this.angle;
        light.distance = this.distance;
        light.markDirty();
        return light;
    }

    @Override
    public void renderImGuiAttributes() {
        Vector3f orientationAngles = this.orientation.getEulerAnglesXYZ(new Vector3f());

        float[] editSize = new float[]{this.size.x(), this.size.y()};

        ImDouble editX = new ImDouble(this.position.x());
        ImDouble editY = new ImDouble(this.position.y());
        ImDouble editZ = new ImDouble(this.position.z());

        float[] editXRot = new float[]{orientationAngles.x()};
        float[] editYRot = new float[]{orientationAngles.y()};
        float[] editZRot = new float[]{orientationAngles.z()};

        float[] editAngle = new float[]{this.angle};
        ImFloat editDistance = new ImFloat(this.distance);

        if (ImGui.dragFloat2("size", editSize, 0.02F, 0.0001F)) {
            this.setSize(editSize[0], editSize[1]);
        }

        float totalWidth = ImGui.calcItemWidth();
        ImGui.pushItemWidth(totalWidth / 3.0F - (ImGui.getStyle().getItemInnerSpacingX() * 0.58F));
        if (ImGui.dragScalar("##x", ImGuiDataType.Double, editX, 0.02F)) {
            this.setPosition(editX.get(), this.position.y(), this.position.z());
        }
        ImGui.sameLine(0, ImGui.getStyle().getItemInnerSpacingX());
        if (ImGui.dragScalar("##y", ImGuiDataType.Double, editY, 0.02F)) {
            this.setPosition(this.position.x(), editY.get(), this.position.z());
        }
        ImGui.sameLine(0, ImGui.getStyle().getItemInnerSpacingX());
        if (ImGui.dragScalar("##z", ImGuiDataType.Double, editZ, 0.02F)) {
            this.setPosition(this.position.x(), this.position.y(), editZ.get());
        }

        ImGui.popItemWidth();
        ImGui.sameLine(0, ImGui.getStyle().getItemInnerSpacingX());
        ImGui.text("position");

        ImGui.pushItemWidth(totalWidth / 3.0F - (ImGui.getStyle().getItemInnerSpacingX() * 0.58F));
        if (ImGui.sliderAngle("##xrot", editXRot)) {
            this.setOrientation(new Quaternionf().rotationXYZ(editXRot[0], orientationAngles.y(), orientationAngles.z()));
        }
        ImGui.sameLine(0, ImGui.getStyle().getItemInnerSpacingX());
        if (ImGui.sliderAngle("##yrot", editYRot)) {
            this.setOrientation(new Quaternionf().rotationXYZ(orientationAngles.x(), editYRot[0], orientationAngles.z()));
        }
        ImGui.sameLine(0, ImGui.getStyle().getItemInnerSpacingX());
        if (ImGui.sliderAngle("##zrot", editZRot)) {
            this.setOrientation(new Quaternionf().rotationXYZ(orientationAngles.x(), orientationAngles.y(), editZRot[0]));
        }

        ImGui.popItemWidth();
        ImGui.sameLine(0, ImGui.getStyle().getItemInnerSpacingX());
        ImGui.text("orientation");

        if (ImGui.sliderAngle("##angle", editAngle, 0.1F, 180.0F, "%.1f")) {
            this.setAngle(editAngle[0]);
        }
        ImGui.sameLine(0, ImGui.getStyle().getItemInnerSpacingX());
        ImGui.text("angle");

        if (ImGui.dragScalar("distance", ImGuiDataType.Float, editDistance, 0.02F, 0.0F)) {
            this.setDistance(editDistance.get());
        }
    }
}
