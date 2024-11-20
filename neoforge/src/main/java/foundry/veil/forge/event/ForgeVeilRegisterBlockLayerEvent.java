package foundry.veil.forge.event;

import foundry.veil.api.event.VeilRegisterBlockLayerEvent;
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
 * @see VeilRegisterBlockLayerEvent
 */
public class ForgeVeilRegisterBlockLayerEvent extends Event implements VeilRegisterBlockLayerEvent.Registry, IModBusEvent {

    private final VeilRegisterBlockLayerEvent.Registry registry;

    public ForgeVeilRegisterBlockLayerEvent(VeilRegisterBlockLayerEvent.Registry registry) {
        this.registry = registry;
    }

    @Override
    public void registerBlockLayer(RenderType renderType) {
        this.registry.registerBlockLayer(renderType);
    }
}
