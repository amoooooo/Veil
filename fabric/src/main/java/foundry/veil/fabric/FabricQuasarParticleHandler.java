package foundry.veil.fabric;

import foundry.veil.api.client.render.CachedBufferSource;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.fabric.event.FabricFreeNativeResourcesEvent;
import foundry.veil.fabric.event.FabricVeilRenderLevelStageEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class FabricQuasarParticleHandler {

    private static CachedBufferSource cachedBufferSource;

    private static void free() {
        if (cachedBufferSource != null) {
            cachedBufferSource.free();
            cachedBufferSource = null;
        }
    }

    public static void setLevel(ClientLevel level) {
        VeilRenderSystem.renderer().getParticleManager().setLevel(level);
        free();
    }

    public static void init() {
        FabricFreeNativeResourcesEvent.EVENT.register(FabricQuasarParticleHandler::free);
        FabricVeilRenderLevelStageEvent.EVENT.register((stage, levelRenderer, bufferSource, poseStack, modelMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum) -> {
            if (stage == VeilRenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                if (cachedBufferSource == null) {
                    cachedBufferSource = new CachedBufferSource();
                }
                VeilRenderSystem.renderer().getParticleManager().render(poseStack, cachedBufferSource, camera, VeilRenderer.getCullingFrustum(), deltaTracker.getRealtimeDeltaTicks());
                cachedBufferSource.endBatch();
            }
        });
    }
}
