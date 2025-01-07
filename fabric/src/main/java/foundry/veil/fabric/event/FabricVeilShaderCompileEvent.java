package foundry.veil.fabric.event;

import foundry.veil.api.event.VeilShaderCompileEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired when Veil has finished compiling shaders.
 *
 * @author Ocelot
 */
@FunctionalInterface
public interface FabricVeilShaderCompileEvent extends VeilShaderCompileEvent {

    Event<VeilShaderCompileEvent> EVENT = EventFactory.createArrayBacked(VeilShaderCompileEvent.class, events -> (shaderManager, updatedPrograms) -> {
        for (VeilShaderCompileEvent event : events) {
            event.onVeilCompileShaders(shaderManager, updatedPrograms);
        }
    });
}
