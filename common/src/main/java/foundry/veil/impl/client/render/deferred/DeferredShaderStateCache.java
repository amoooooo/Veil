package foundry.veil.impl.client.render.deferred;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.shader.ShaderProgramImpl;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@ApiStatus.Internal
public class DeferredShaderStateCache {

    private Object veil$oldShader;
    private ShaderProgram veil$deferredShader;
    private boolean printedError;

    /**
     * Sets up the render state for the specified shader instance.
     *
     * @param shaderInstance The shader to set the render state for
     * @return Whether the state was handled and set
     */
    public boolean setupRenderState(@Nullable ShaderInstance shaderInstance) {
        ShaderInstance shader = this.getShader(shaderInstance);
        if (shader != shaderInstance) {
            if (shader instanceof ShaderProgramImpl.Wrapper wrapper) {
                VeilRenderSystem.setShader(wrapper.program());
            } else {
                RenderSystem.setShader(() -> shader);
            }
            return true;
        }
        return false;
    }

    /**
     * Sets up the render state for the specified shader instance.
     *
     * @param shaderInstance The shader to set the render state for
     * @return Whether the state was handled and set
     */
    public boolean setupRenderState(@Nullable ShaderProgram shaderInstance) {
        ShaderProgram shader = this.getShader(shaderInstance);
        if (shader != shaderInstance) {
            VeilRenderSystem.setShader(shader);
            return true;
        }
        return false;
    }

    /**
     * Retrieves the shader that should be used if using deferred rendering
     *
     * @param shaderInstance The shader to get the render state for
     * @return The shader to use
     */
    @Contract("null -> null")
    public ShaderInstance getShader(@Nullable ShaderInstance shaderInstance) {
        if (shaderInstance == null || !VeilRenderSystem.renderer().getDeferredRenderer().isActive()) {
            this.veil$oldShader = null;
            this.veil$deferredShader = null;
            return shaderInstance;
        }

        if (!Objects.equals(this.veil$oldShader, shaderInstance)) {
            this.veil$oldShader = shaderInstance;
            ResourceLocation id = ResourceLocation.parse(shaderInstance.getName());
            this.veil$deferredShader = VeilRenderSystem.renderer().getDeferredRenderer().getDeferredShaderManager().getShader(id);

            if (Veil.platform().isDevelopmentEnvironment()) {
                if (this.veil$deferredShader != null) {
                    this.printedError = false;
                } else {
                    if (!this.printedError) {
                        Veil.LOGGER.warn("Failed to find deferred shader for vanilla shader: {}", id);
                        this.printedError = true;
                    }
                }
            }
        }
        return this.veil$deferredShader != null ? this.veil$deferredShader.toShaderInstance() : shaderInstance;
    }

    /**
     * Retrieves the shader that should be used if using deferred rendering
     *
     * @param shaderProgram The shader to get the render state for
     * @return The shader to use
     */
    @Contract("null -> null")
    public ShaderProgram getShader(@Nullable ShaderProgram shaderProgram) {
        if (shaderProgram == null || !VeilRenderSystem.renderer().getDeferredRenderer().isActive()) {
            this.veil$oldShader = null;
            this.veil$deferredShader = null;
            return shaderProgram;
        }

        if (!Objects.equals(this.veil$oldShader, shaderProgram)) {
            this.veil$oldShader = shaderProgram;
            this.veil$deferredShader = VeilRenderSystem.renderer().getDeferredRenderer().getDeferredShaderManager().getShader(shaderProgram.getId());

            if (Veil.platform().isDevelopmentEnvironment()) {
                if (this.veil$deferredShader != null) {
                    this.printedError = false;
                } else {
                    if (!this.printedError) {
                        Veil.LOGGER.warn("Failed to find deferred shader for veil shader: {}", shaderProgram.getId());
                        this.printedError = true;
                    }
                }
            }
        }
        return Objects.requireNonNullElse(this.veil$deferredShader, shaderProgram);
    }
}
