package foundry.veil.api.event;

import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;

/**
 * Fired when Veil is about to compile shaders.
 *
 * @author Ocelot
 * @see ShaderPreProcessor
 */
@FunctionalInterface
public interface VeilAddShaderPreProcessorsEvent {

    /**
     * Called when the shader manager is about to compile shaders.
     *
     * @param shaderManager The shader manager instance preparing processors
     * @param registry      The register to add shader pre-processors to
     */
    void onRegisterShaderPreProcessors(ShaderManager shaderManager, Registry registry);

    /**
     * Registers shader pre-processors.
     *
     * @author Ocelot
     */
    @FunctionalInterface
    interface Registry {

        /**
         * Adds the specified pre-processor to the end of the stack.
         *
         * @param processor     The processor to add
         * @param modifyImports Whether the processor will also be run on imports
         */
        void addPreprocessor(ShaderPreProcessor processor, boolean modifyImports);

        /**
         * Adds the specified pre-processor to the end of the stack.
         *
         * @param processor The processor to add
         */
        default void addPreprocessor(ShaderPreProcessor processor) {
            this.addPreprocessor(processor, true);
        }
    }
}
