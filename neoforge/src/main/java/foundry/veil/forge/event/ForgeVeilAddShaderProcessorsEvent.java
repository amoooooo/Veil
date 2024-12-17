package foundry.veil.forge.event;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.event.VeilAddShaderPreProcessorsEvent;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.bus.api.Event;

/**
 * Fired when Minecraft frees all native resources on the client.
 *
 * @author Ocelot
 */
public class ForgeVeilAddShaderProcessorsEvent extends Event implements VeilAddShaderPreProcessorsEvent.Registry {

    private final ResourceProvider resourceProvider;
    private final VeilAddShaderPreProcessorsEvent.Registry registry;

    public ForgeVeilAddShaderProcessorsEvent(ResourceProvider resourceProvider, VeilAddShaderPreProcessorsEvent.Registry registry) {
        this.resourceProvider = resourceProvider;
        this.registry = registry;
    }

    /**
     * @return Access to retrieving resources
     */
    public ResourceProvider getResourceProvider() {
        return this.resourceProvider;
    }

    @Override
    public void addPreprocessorFirst(ShaderPreProcessor processor) {
        this.registry.addPreprocessorFirst(processor);
    }

    @Override
    public void addPreprocessorFirst(ShaderPreProcessor processor, boolean modifyImports) {
        this.registry.addPreprocessorFirst(processor, modifyImports);
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
