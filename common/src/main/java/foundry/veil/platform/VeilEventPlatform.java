package foundry.veil.platform;

import foundry.veil.api.event.*;

import java.util.ServiceLoader;

/**
 * Manages platform-specific implementations of event subscriptions.
 *
 * @author Ocelot
 */
public interface VeilEventPlatform {

    VeilEventPlatform INSTANCE = ServiceLoader.load(VeilEventPlatform.class).findFirst().orElseThrow(() -> new RuntimeException("Failed to find platform event provider"));

    void onFreeNativeResources(FreeNativeResourcesEvent event);

    void onVeilAddShaderProcessors(VeilAddShaderPreProcessorsEvent event);

    void preVeilPostProcessing(VeilPostProcessingEvent.Pre event);

    void postVeilPostProcessing(VeilPostProcessingEvent.Post event);

    void onVeilRegisterBlockLayers(VeilRegisterBlockLayersEvent event);

    void onVeilRegisterFixedBuffers(VeilRegisterFixedBuffersEvent event);

    void onVeilRendererAvailable(VeilRendererAvailableEvent event);

    void onVeilRenderLevelStage(VeilRenderLevelStageEvent event);
}
