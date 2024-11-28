package foundry.veil.fabric.compat.iris;

import foundry.veil.fabric.mixin.compat.iris.IrisRenderingPipelineAccessor;
import foundry.veil.impl.compat.IrisCompat;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.renderer.ShaderInstance;

import java.util.Collections;
import java.util.Set;

public class VeilFabricIrisCompat implements IrisCompat {

    @Override
    public Set<ShaderInstance> getLoadedShaders() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof IrisRenderingPipelineAccessor accessor) {
            return accessor.getLoadedShaders();
        }
        return Collections.emptySet();
    }

    @Override
    public boolean areShadersLoaded() {
        return Iris.getCurrentPack().isPresent();
    }

    @Override
    public void recompile() {
        // TODO
    }
}
