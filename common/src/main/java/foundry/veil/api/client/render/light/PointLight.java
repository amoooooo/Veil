package foundry.veil.api.client.render.light;

import foundry.veil.api.client.editor.EditorAttributeProvider;
import foundry.veil.api.client.registry.LightTypeRegistry;
import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.type.ImDouble;
import imgui.type.ImFloat;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import java.nio.ByteBuffer;

/**
 * Represents a light where all rays come from a position in space.
 *
 * @author Ocelot
 */
public class PointLight extends Light implements IndirectLight<PointLight>, EditorAttributeProvider {

    protected final Vector3d position;
    protected float radius;

    public PointLight() {
        this.position = new Vector3d();
        this.radius = 1.0F;
    }

    @Override
    public void store(ByteBuffer buffer) {
        this.position.getf(buffer.position(), buffer);
        buffer.position(buffer.position() + Float.BYTES * 3);
        buffer.putFloat(this.color.x() * this.brightness);
        buffer.putFloat(this.color.y() * this.brightness);
        buffer.putFloat(this.color.z() * this.brightness);
        buffer.putFloat(this.radius);
    }

    @Override
    public Vector3dc getPosition() {
        return this.position;
    }

    @Override
    public float getRadius() {
        return this.radius;
    }

    @Override
    public PointLight setColor(float red, float green, float blue) {
        return (PointLight) super.setColor(red, green, blue);
    }

    @Override
    public PointLight setColor(Vector3fc color) {
        return (PointLight) super.setColor(color);
    }

    @Override
    public PointLight setBrightness(float brightness) {
        return (PointLight) super.setBrightness(brightness);
    }

    @Override
    public PointLight setPosition(double x, double y, double z) {
        this.position.set(x, y, z);
        this.markDirty();
        return this;
    }

    @Override
    public PointLight setRadius(float radius) {
        this.radius = radius;
        this.markDirty();
        return this;
    }

    @Override
    public PointLight setTo(Camera camera) {
        Vec3 pos = camera.getPosition();
        return this.setPosition(pos.x, pos.y, pos.z);
    }

    @Override
    public LightTypeRegistry.LightType<?> getType() {
        return LightTypeRegistry.POINT.get();
    }

    @Override
    public void renderImGuiAttributes() {
        ImDouble editX = new ImDouble(this.position.x());
        ImDouble editY = new ImDouble(this.position.y());
        ImDouble editZ = new ImDouble(this.position.z());

        ImFloat editRadius = new ImFloat(this.radius);

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

        if (ImGui.dragScalar("radius", ImGuiDataType.Float, editRadius, 0.02F, 0.0F)) {
            this.setRadius(editRadius.get());
        }
    }
}
