package foundry.veil.forge.event;

import foundry.veil.api.event.VeilRegisterBlockLayersEvent;
import foundry.veil.api.event.VeilRegisterFixedBuffersEvent;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * <p>Fired to register block layers.</p>
 * <p><strong>Note: This does not add fixed buffers automatically. To do that, also register the render operand with {@link VeilRegisterFixedBuffersEvent}</strong></p>
 *
 * @author Ocelot
 * @see VeilRegisterFixedBuffersEvent
 * @see VeilRegisterBlockLayersEvent
 */
public class ForgeVeilRegisterBlockLayersEvent extends Event implements VeilRegisterBlockLayersEvent.Registry, IModBusEvent {

    private final VeilRegisterBlockLayersEvent.Registry registry;

    public ForgeVeilRegisterBlockLayersEvent(VeilRegisterBlockLayersEvent.Registry registry) {
        this.registry = registry;
    }

    @Override
    public void registerBlockLayer(RenderType renderType) {
        this.registry.registerBlockLayer(renderType);
    }
}
