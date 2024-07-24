package foundry.veil.platform;

import foundry.veil.api.event.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.ServiceLoader;

/**
 * Manages platform-specific implementations of event subscriptions.
 *
 * @author Ocelot
 */
public interface VeilEventPlatform {

    VeilEventPlatform INSTANCE = ServiceLoader.load(VeilEventPlatform.class).findFirst().orElseThrow(() -> new RuntimeException("Failed to find platform event provider"));

    void onFreeNativeResources(FreeNativeResourcesEvent event);

    void onVeilRendererAvailable(VeilRendererEvent event);

    void preVeilPostProcessing(VeilPostProcessingEvent.Pre event);

    void postVeilPostProcessing(VeilPostProcessingEvent.Post event);

    void onVeilRegisterFixedBuffers(VeilRegisterFixedBuffersEvent event);

    void onVeilRegisterBlockLayers(VeilRegisterBlockLayerEvent event);

    void onVeilRenderTypeStageRender(VeilRenderLevelStageEvent event);

    void onVeilAssignBlockRenderLayer(VeilAssignRenderLayerEvent<Block> event);

    void onVeilAssignFluidRenderLayer(VeilAssignRenderLayerEvent<Fluid> event);

    void onVeilAssignItemRenderLayer(VeilAssignRenderLayerEvent<Item> event);
}
