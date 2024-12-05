package foundry.veil.api.event;

import net.minecraft.client.renderer.RenderType;

/**
 * <p>Fired to register block layers.</p>
 * <p><strong>Note: This does not add fixed buffers automatically. To do that, also register the render type with {@link VeilRegisterFixedBuffersEvent}</strong></p>
 *
 * @author Ocelot
 * @see VeilRegisterFixedBuffersEvent
 */
@FunctionalInterface
public interface VeilRegisterBlockLayersEvent {

    /**
     * Registers custom block render layers.
     *
     * @param registry The registry to add render types to
     */
    void onRegisterBlockLayers(Registry registry);

    /**
     * Registers additional fixed render buffers.
     *
     * @author Ocelot
     */
    @FunctionalInterface
    interface Registry {

        /**
         * <p>Registers a custom block render layer. It should use a small buffer size (256) to reduce the memory allocated to building that layer.</p>
         * <p>To actually render the layer, {@link VeilRegisterFixedBuffersEvent.Registry#registerFixedBuffer(VeilRenderLevelStageEvent.Stage, RenderType)} must be called.</p>
         *
         * @param renderType The render type to add to the block
         */
        void registerBlockLayer(RenderType renderType);
    }
}
