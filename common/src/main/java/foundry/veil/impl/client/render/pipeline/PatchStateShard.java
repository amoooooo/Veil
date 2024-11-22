package foundry.veil.impl.client.render.pipeline;

import foundry.veil.Veil;
import net.minecraft.client.renderer.RenderStateShard;
import org.jetbrains.annotations.ApiStatus;

import static org.lwjgl.opengl.GL40C.GL_PATCH_VERTICES;
import static org.lwjgl.opengl.GL40C.glPatchParameteri;

@ApiStatus.Internal
public class PatchStateShard extends RenderStateShard {

    private final int patchVertices;

    public PatchStateShard(int patchVertices) {
        super(Veil.MODID + ":patches", () -> glPatchParameteri(GL_PATCH_VERTICES, patchVertices), () -> glPatchParameteri(GL_PATCH_VERTICES, 1));
        this.patchVertices = patchVertices;
    }

    @Override
    public String toString() {
        return this.name + "[" + this.patchVertices + "]";
    }
}
