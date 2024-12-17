package foundry.veil.fabric.platform;

import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.fabric.event.FabricVeilAddShaderPreProcessorsEvent;
import foundry.veil.fabric.event.FabricVeilPostProcessingEvent;
import foundry.veil.platform.VeilClientPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class FabricVeilClientPlatform implements VeilClientPlatform {

    @Override
    public void preVeilPostProcessing(ResourceLocation name, PostPipeline pipeline, PostPipeline.Context context) {
        FabricVeilPostProcessingEvent.PRE.invoker().preVeilPostProcessing(name, pipeline, context);
    }

    @Override
    public void postVeilPostProcessing(ResourceLocation name, PostPipeline pipeline, PostPipeline.Context context) {
        FabricVeilPostProcessingEvent.POST.invoker().postVeilPostProcessing(name, pipeline, context);
    }

    @Override
    public void onRegisterShaderPreProcessors(ResourceProvider resourceProvider, Registry registry) {
        FabricVeilAddShaderPreProcessorsEvent.EVENT.invoker().onRegisterShaderPreProcessors(resourceProvider, registry);
    }
}
