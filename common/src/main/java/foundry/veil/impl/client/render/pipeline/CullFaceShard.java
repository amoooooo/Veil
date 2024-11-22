package foundry.veil.impl.client.render.pipeline;

import foundry.veil.Veil;
import net.minecraft.client.renderer.RenderStateShard;
import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;

import static org.lwjgl.opengl.GL11C.*;

@ApiStatus.Internal
public class CullFaceShard extends RenderStateShard {

    private final String modeName;

    public CullFaceShard(int mode) {
        super(Veil.MODID + ":cull_face", () -> glCullFace(mode), () -> glCullFace(GL_BACK));
        this.modeName = switch (mode) {
            case GL_FRONT -> "front";
            case GL_BACK -> "back";
            case GL_FRONT_AND_BACK -> "front_and_back";
            default -> "0x" + Integer.toHexString(mode).toLowerCase(Locale.ROOT);
        };
    }

    @Override
    public String toString() {
        return this.name + "[" + this.modeName + "]";
    }
}
