package foundry.veil.api.event;

import foundry.veil.api.client.render.VeilRenderer;

/**
 * Fired when Veil has finished initialization and the renderer is safe to use.
 *
 * @author Ocelot
 */
@FunctionalInterface
public interface VeilRendererAvailableEvent {

    /**
     * Called when the Veil renderer is now available.
     *
     * @param renderer The renderer instance
     */
    void onVeilRendererAvailable(VeilRenderer renderer);
}
