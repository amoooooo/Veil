package foundry.veil.api.client.render.light;

import foundry.veil.api.client.editor.EditorAttributeProvider;
import foundry.veil.api.client.registry.LightTypeRegistry;
import imgui.ImGui;
import net.minecraft.client.Camera;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Represents a light where all rays come from the same direction everywhere. (The sun)
 */
public class DirectionalLight extends Light implements EditorAttributeProvider {

    protected final Vector3f direction;

    public DirectionalLight() {
        this.direction = new Vector3f(0.0F, -1.0F, 0.0F);
    }

    /**
     * @return The direction this light is facing
     */
    public Vector3fc getDirection() {
        return this.direction;
    }

    @Override
    public DirectionalLight setColor(float red, float green, float blue) {
        return (DirectionalLight) super.setColor(red, green, blue);
    }

    @Override
    public DirectionalLight setColor(Vector3fc color) {
        return (DirectionalLight) super.setColor(color);
    }

    @Override
    public DirectionalLight setColor(int color) {
        return (DirectionalLight) super.setColor(color);
    }

    /**
     * Sets the direction of this light.
     *
     * @param direction The new direction
     */
    public DirectionalLight setDirection(Vector3fc direction) {
        return this.setDirection(direction.x(), direction.y(), direction.z());
    }

    /**
     * Sets the direction of this light.
     *
     * @param x The new x direction
     * @param y The new y direction
     * @param z The new z direction
     */
    public DirectionalLight setDirection(float x, float y, float z) {
        this.direction.set(x, y, z);
        this.markDirty();
        return this;
    }

    @Override
    public DirectionalLight setTo(Camera camera) {
        Vector3f look = camera.getLookVector();
        return this.setDirection(look.x, look.y, look.z);
    }

    @Override
    public LightTypeRegistry.LightType<?> getType() {
        return LightTypeRegistry.DIRECTIONAL.get();
    }

    @Override
    public DirectionalLight clone() {
        return new DirectionalLight()
                .setColor(this.color)
                .setDirection(this.direction);
    }

    @Override
    public void renderImGuiAttributes() {
        float[] editDirection = new float[]{this.direction.x(), this.direction.y(), this.direction.z()};

        if (ImGui.sliderFloat3("##direction", editDirection, -1.0F, 1.0F)) {
            this.setDirection(editDirection[0], editDirection[1], editDirection[2]);
        }
        ImGui.sameLine(0, ImGui.getStyle().getItemInnerSpacingX());
        ImGui.text("direction");
    }
}
