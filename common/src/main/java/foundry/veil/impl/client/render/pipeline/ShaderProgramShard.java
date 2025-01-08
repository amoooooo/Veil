package foundry.veil.impl.client.render.pipeline;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.renderer.RenderStateShard;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

@ApiStatus.Internal
public class ShaderProgramShard extends RenderStateShard.ShaderStateShard {

    private final Supplier<ShaderProgram> shader;

    public ShaderProgramShard(Supplier<ShaderProgram> shader) {
        this.shader = shader;
    }

    @Override
    public void setupRenderState() {
        VeilRenderSystem.setShader(this.shader);
    }

    @Override
    public String toString() {
        ShaderProgram shader = this.shader.get();
        return this.name + "[" + (shader != null ? shader.getName() : "null") + "]";
    }
}
