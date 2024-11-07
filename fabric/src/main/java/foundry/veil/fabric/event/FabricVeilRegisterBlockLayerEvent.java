package foundry.veil.fabric.event;

import foundry.veil.api.event.VeilRegisterBlockLayerEvent;
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
public interface FabricVeilRegisterBlockLayerEvent extends VeilRegisterBlockLayerEvent {

    Event<VeilRegisterBlockLayerEvent> EVENT = EventFactory.createArrayBacked(VeilRegisterBlockLayerEvent.class, events -> registry -> {
        for (VeilRegisterBlockLayerEvent event : events) {
            event.onRegisterBlockLayers(registry);
        }
    });
}
