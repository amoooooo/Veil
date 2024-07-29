package foundry.veil.fabric.platform;

import foundry.veil.api.event.*;
import foundry.veil.fabric.event.*;
import foundry.veil.platform.VeilEventPlatform;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class FabricVeilEventPlatform implements VeilEventPlatform {

    @Override
    public void onFreeNativeResources(FreeNativeResourcesEvent event) {
        FabricFreeNativeResourcesEvent.EVENT.register(event);
    }

    @Override
    public void onVeilRendererAvailable(VeilRendererEvent event) {
        FabricVeilRendererEvent.EVENT.register(event);
    }

    @Override
    public void preVeilPostProcessing(VeilPostProcessingEvent.Pre event) {
        FabricVeilPostProcessingEvent.PRE.register(event);
    }

    @Override
    public void postVeilPostProcessing(VeilPostProcessingEvent.Post event) {
        FabricVeilPostProcessingEvent.POST.register(event);
    }

    @Override
    public void onVeilRegisterFixedBuffers(VeilRegisterFixedBuffersEvent event) {
        FabricVeilRegisterFixedBuffersEvent.EVENT.register(event);
    }

    @Override
    public void onVeilRegisterBlockLayers(VeilRegisterBlockLayerEvent event) {
        FabricVeilRegisterBlockLayerEvent.EVENT.register(event);
    }

    @Override
    public void onVeilRenderTypeStageRender(VeilRenderLevelStageEvent event) {
        FabricVeilRenderLevelStageEvent.EVENT.register(event);
    }
}
