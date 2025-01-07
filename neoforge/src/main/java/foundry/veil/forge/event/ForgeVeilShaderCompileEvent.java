package foundry.veil.forge.event;

import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.Collection;
import java.util.Map;

/**
 * Fired when Veil has finished compiling shaders.
 *
 * @author Ocelot
 */
public class ForgeVeilShaderCompileEvent extends Event implements IModBusEvent {

    private final ShaderManager shaderManager;
    private final Map<ResourceLocation, ShaderProgram> updatedPrograms;

    public ForgeVeilShaderCompileEvent(ShaderManager shaderManager, Map<ResourceLocation, ShaderProgram> updatedPrograms) {
        this.shaderManager = shaderManager;
        this.updatedPrograms = updatedPrograms;
    }

    /**
     * @return The shader manager instance
     */
    public ShaderManager getShaderManager() {
        return this.shaderManager;
    }

    /**
     * @return A view of the programs that have changed
     */
    public Map<ResourceLocation, ShaderProgram> getUpdatedPrograms() {
        return this.updatedPrograms;
    }
}
