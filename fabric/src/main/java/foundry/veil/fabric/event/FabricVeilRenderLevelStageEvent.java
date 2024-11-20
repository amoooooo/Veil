package foundry.veil.fabric.event;

import foundry.veil.api.event.VeilRegisterFixedBuffersEvent;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired for each render stage to draw arbitrarily to the screen. This is available as a last-resort if {@link VeilRegisterFixedBuffersEvent} doesn't fit the use case.
 *
 * @author Ocelot
 * @see FabricVeilRegisterFixedBuffersEvent
 */
@FunctionalInterface
public interface FabricVeilRenderLevelStageEvent extends VeilRenderLevelStageEvent {

    Event<VeilRenderLevelStageEvent> EVENT = EventFactory.createArrayBacked(VeilRenderLevelStageEvent.class, events -> (stage, levelRenderer, bufferSource, matrixStack, frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum) -> {
        for (VeilRenderLevelStageEvent event : events) {
            event.onRenderLevelStage(stage, levelRenderer, bufferSource, matrixStack, frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum);
        }
    });
}
