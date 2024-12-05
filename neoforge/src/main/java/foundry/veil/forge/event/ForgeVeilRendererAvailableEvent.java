package foundry.veil.forge.event;

import foundry.veil.api.client.render.VeilRenderer;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Fired when Veil has finished initialization and the renderer is safe to use.
 *
 * @author Ocelot
 */
public class ForgeVeilRendererAvailableEvent extends Event implements IModBusEvent {

    private final VeilRenderer renderer;

    public ForgeVeilRendererAvailableEvent(VeilRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * @return The renderer instance
     */
    public VeilRenderer getRenderer() {
        return this.renderer;
    }
}
