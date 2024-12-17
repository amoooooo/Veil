package foundry.veil.api.event;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import net.minecraft.server.packs.resources.ResourceProvider;

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
     * @param resourceProvider Access to retrieving resources
     * @param registry         The register to add shader pre-processors to
     */
    void onRegisterShaderPreProcessors(ResourceProvider resourceProvider, Registry registry);

    /**
     * Registers shader pre-processors.
     *
     * @author Ocelot
     */
    interface Registry {

        /**
         * Adds the specified pre-processor to the start of the stack.
         *
         * @param processor     The processor to add
         * @param modifyImports Whether the processor will also be run on imports
         */
        void addPreprocessorFirst(ShaderPreProcessor processor, boolean modifyImports);

        /**
         * Adds the specified pre-processor to the start of the stack.
         *
         * @param processor The processor to add
         */
        default void addPreprocessorFirst(ShaderPreProcessor processor) {
            this.addPreprocessor(processor, true);
        }

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
