package foundry.veil.impl.client.render.perspective;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.util.Objects;

@ApiStatus.Internal
public class LevelPerspectiveCamera extends Camera {

    private static final Vector3f EULER_ANGLES = new Vector3f();
    private float viewDistance;

    public void setup(Vector3dc position, @Nullable Entity cameraEntity, ClientLevel level, Quaternionfc orientation, float viewDistance) {
        super.setup(level, cameraEntity != null ? cameraEntity : Objects.requireNonNull(Minecraft.getInstance().player), true, false, 1.0F);
        this.setPosition(position.x(), position.y(), position.z());

        orientation.getEulerAnglesYXZ(EULER_ANGLES);
        super.setRotation((float) (-EULER_ANGLES.y * 180 / Math.PI), (float) (EULER_ANGLES.x * 180 / Math.PI));
        this.rotation().set(orientation);
        this.getLookVector().set(0.0F, 0.0F, -1.0F).rotate(orientation);
        this.getUpVector().set(0.0F, 1.0F, 0.0F).rotate(orientation);
        this.getLeftVector().set(-1.0F, 0.0F, 0.0F).rotate(orientation);
        this.viewDistance = viewDistance;
    }

    public float getRenderDistance() {
        return this.viewDistance;
    }
}
