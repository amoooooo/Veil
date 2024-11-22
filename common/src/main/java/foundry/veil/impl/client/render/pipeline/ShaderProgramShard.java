package foundry.veil.impl.client.render.pipeline;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.deferred.DeferredShaderStateCache;
import net.minecraft.client.renderer.RenderStateShard;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

@ApiStatus.Internal
public class ShaderProgramShard extends RenderStateShard.ShaderStateShard {

    private final Supplier<ShaderProgram> shader;
    private final DeferredShaderStateCache cache;

    public ShaderProgramShard(Supplier<ShaderProgram> shader) {
        this.shader = shader;
        this.cache = new DeferredShaderStateCache();
    }

    @Override
    public void setupRenderState() {
        ShaderProgram program = this.shader.get();
        if (!this.cache.setupRenderState(program)) {
            VeilRenderSystem.setShader(program);
        }
    }

    @Override
    public String toString() {
        ShaderProgram shader = this.shader.get();
        return this.name + "[" + (shader != null ? shader.getId() : "null") + "]";
    }
}
