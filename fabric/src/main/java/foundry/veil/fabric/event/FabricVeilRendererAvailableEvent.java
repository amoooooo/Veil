package foundry.veil.fabric.event;

import foundry.veil.api.event.VeilRendererAvailableEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired when Veil has finished initialization and the renderer is safe to use.
 *
 * @author Ocelot
 */
@FunctionalInterface
public interface FabricVeilRendererAvailableEvent extends VeilRendererAvailableEvent {

    Event<VeilRendererAvailableEvent> EVENT = EventFactory.createArrayBacked(VeilRendererAvailableEvent.class, events -> renderer -> {
        for (VeilRendererAvailableEvent event : events) {
            event.onVeilRendererAvailable(renderer);
        }
    });
}
