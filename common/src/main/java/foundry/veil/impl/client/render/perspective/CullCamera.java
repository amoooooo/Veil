package foundry.veil.impl.client.render.perspective;

import net.minecraft.client.Camera;

public class CullCamera extends Camera {

    public CullCamera(Camera camera) {
        this.setPosition(camera.getPosition());
    }
}
