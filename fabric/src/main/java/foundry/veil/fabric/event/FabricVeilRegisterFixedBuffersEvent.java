package foundry.veil.fabric.event;

import foundry.veil.api.event.VeilRegisterFixedBuffersEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * <p>Fired to register additional fixed render types</p>
 * <p>Fixed buffers are batched together and are not drawn until after the specified stage is drawn. This should be used in most cases to defer a specific render operand to a specific time.</p>
 *
 * @author Ocelot
 */
@FunctionalInterface
public interface FabricVeilRegisterFixedBuffersEvent extends VeilRegisterFixedBuffersEvent {

    Event<VeilRegisterFixedBuffersEvent> EVENT = EventFactory.createArrayBacked(VeilRegisterFixedBuffersEvent.class, events -> registry -> {
        for (VeilRegisterFixedBuffersEvent event : events) {
            event.onRegisterFixedBuffers(registry);
        }
    });
}
