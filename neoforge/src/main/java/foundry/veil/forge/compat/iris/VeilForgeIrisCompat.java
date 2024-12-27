package foundry.veil.forge.compat.iris;

import foundry.veil.ext.iris.IrisRenderTargetExtension;
import foundry.veil.forge.mixin.compat.iris.IrisRenderingPipelineAccessor;
import foundry.veil.api.compat.IrisCompat;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.renderer.ShaderInstance;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class VeilForgeIrisCompat implements IrisCompat {

    @Override
    public Set<ShaderInstance> getLoadedShaders() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof IrisRenderingPipelineAccessor accessor) {
            return accessor.getLoadedShaders();
        }
        return Collections.emptySet();
    }

    @Override
    public Map<String, IrisRenderTargetExtension> getRenderTargets() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof IrisRenderingPipelineAccessor accessor) {
            RenderTargets renderTargets = accessor.getRenderTargets();
            Object2ObjectArrayMap<String, IrisRenderTargetExtension> extensions = new Object2ObjectArrayMap<>(renderTargets.getRenderTargetCount());
            for (int i = 0; i < renderTargets.getRenderTargetCount(); i++) {
                IrisRenderTargetExtension ext = (IrisRenderTargetExtension) renderTargets.get(i);
                if (ext != null) {
                    extensions.put(ext.veil$getName(), ext);
                }
            }
            return extensions;
        }
        return Object2ObjectMaps.emptyMap();
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
