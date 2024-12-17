package foundry.veil.fabric.mixin.compat.iris;

import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(IrisRenderingPipeline.class)
public interface IrisRenderingPipelineAccessor {

    @Accessor(remap = false)
    RenderTargets getRenderTargets();

    @Accessor(remap = false)
    Set<ShaderInstance> getLoadedShaders();
}
