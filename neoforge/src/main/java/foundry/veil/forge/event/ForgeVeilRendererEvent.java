package foundry.veil.forge.event;

import foundry.veil.api.client.render.VeilRenderer;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

/**
 * Fired when Veil has finished initialization and the renderer is safe to use.
 *
 * @author Ocelot
 */
public class ForgeVeilRendererEvent extends Event implements IModBusEvent {

    private final VeilRenderer renderer;

    public ForgeVeilRendererEvent(VeilRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * @return The renderer instance
     */
    public VeilRenderer getRenderer() {
        return this.renderer;
    }
}
