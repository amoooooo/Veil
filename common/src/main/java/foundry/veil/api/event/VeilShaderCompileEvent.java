package foundry.veil.api.event;

import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Fired when Veil has finished compiling shaders.
 *
 * @author Ocelot
 */
@FunctionalInterface
public interface VeilShaderCompileEvent {

    /**
     * Called when Veil shaders recompile.
     *
     * @param shaderManager   The shader manager instance
     * @param updatedPrograms A view of the programs that have changed
     */
    void onVeilCompileShaders(ShaderManager shaderManager, Map<ResourceLocation, ShaderProgram> updatedPrograms);
}
