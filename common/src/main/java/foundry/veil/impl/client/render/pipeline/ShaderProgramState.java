package foundry.veil.impl.client.render.pipeline;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.deferred.DeferredShaderStateCache;
import net.minecraft.client.renderer.RenderStateShard;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@ApiStatus.Internal
public class ShaderProgramState extends RenderStateShard.ShaderStateShard {

    private final @Nullable Supplier<ShaderProgram> setup;
    private final DeferredShaderStateCache cache;

    public ShaderProgramState(@Nullable Supplier<ShaderProgram> setup) {
        this.setup = setup;
        this.cache = new DeferredShaderStateCache();
    }

    @Override
    public void setupRenderState() {
        if (this.setup == null)
            VeilRenderSystem.setShader((ShaderProgram) null);
        ShaderProgram program = this.setup.get();
        if (program == null)
            VeilRenderSystem.setShader((ShaderProgram) null);
        if (!this.cache.setupRenderState(program)) {
            VeilRenderSystem.setShader(program);
        }
    }
}
