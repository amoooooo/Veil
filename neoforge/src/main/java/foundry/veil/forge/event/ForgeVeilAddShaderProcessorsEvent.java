package foundry.veil.forge.event;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.event.VeilAddShaderPreProcessorsEvent;
import net.neoforged.bus.api.Event;

/**
 * Fired when Minecraft frees all native resources on the client.
 *
 * @author Ocelot
 */
public class ForgeVeilAddShaderProcessorsEvent extends Event implements VeilAddShaderPreProcessorsEvent.Registry {

    private final VeilAddShaderPreProcessorsEvent.Registry registry;

    public ForgeVeilAddShaderProcessorsEvent(VeilAddShaderPreProcessorsEvent.Registry registry) {
        this.registry = registry;
    }

    @Override
    public void addPreprocessor(ShaderPreProcessor processor, boolean modifyImports) {
        this.registry.addPreprocessor(processor, modifyImports);
    }

    @Override
    public void addPreprocessor(ShaderPreProcessor processor) {
        this.registry.addPreprocessor(processor);
    }
}
