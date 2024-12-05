package foundry.veil.fabric.event;

import foundry.veil.api.event.VeilRegisterBlockLayersEvent;
import foundry.veil.api.event.VeilRegisterFixedBuffersEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * <p>Fired to register block layers.</p>
 * <p><strong>Note: This does not add fixed buffers automatically. To do that, also register the render operand with {@link VeilRegisterFixedBuffersEvent}</strong></p>
 *
 * @author Ocelot
 * @see VeilRegisterFixedBuffersEvent
 */
@FunctionalInterface
public interface FabricVeilRegisterBlockLayersEvent extends VeilRegisterBlockLayersEvent {

    Event<VeilRegisterBlockLayersEvent> EVENT = EventFactory.createArrayBacked(VeilRegisterBlockLayersEvent.class, events -> registry -> {
        for (VeilRegisterBlockLayersEvent event : events) {
            event.onRegisterBlockLayers(registry);
        }
    });
}
