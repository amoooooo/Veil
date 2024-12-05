package foundry.veil.forge.event;

import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.event.VeilAddShaderPreProcessorsEvent;
import net.neoforged.bus.api.Event;

/**
 * Fired when Minecraft frees all native resources on the client.
 *
 * @author Ocelot
 */
public class ForgeVeilAddShaderProcessorsEvent extends Event implements VeilAddShaderPreProcessorsEvent.Registry {

    private final ShaderManager shaderManager;
    private final VeilAddShaderPreProcessorsEvent.Registry registry;

    public ForgeVeilAddShaderProcessorsEvent(ShaderManager shaderManager, VeilAddShaderPreProcessorsEvent.Registry registry) {
        this.shaderManager = shaderManager;
        this.registry = registry;
    }

    /**
     * @return The shader manager instance preparing processors
     */
    public ShaderManager getShaderManager() {
        return this.shaderManager;
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
