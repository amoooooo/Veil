package foundry.veil.fabric.event;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.event.VeilAddShaderPreProcessorsEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired when Veil is about to compile shaders.
 *
 * @author Ocelot
 * @see ShaderPreProcessor
 */
@FunctionalInterface
public interface FabricVeilAddShaderPreProcessorsEvent extends VeilAddShaderPreProcessorsEvent {

    Event<VeilAddShaderPreProcessorsEvent> EVENT = EventFactory.createArrayBacked(VeilAddShaderPreProcessorsEvent.class, (provider, registry) -> {
    }, events -> (provider, registry) -> {
        for (VeilAddShaderPreProcessorsEvent event : events) {
            event.onRegisterShaderPreProcessors(provider, registry);
        }
    });
}
