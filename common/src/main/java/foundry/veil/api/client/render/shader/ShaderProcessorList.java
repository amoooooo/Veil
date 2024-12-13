package foundry.veil.api.client.render.shader;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.event.VeilAddShaderPreProcessorsEvent;

import java.util.LinkedList;
import java.util.List;

public class ShaderProcessorList implements VeilAddShaderPreProcessorsEvent.Registry {

    private final List<ShaderPreProcessor> processors;
    private final List<ShaderPreProcessor> importProcessors;
    private ShaderPreProcessor processor;
    private ShaderPreProcessor importProcessor;

    public ShaderProcessorList() {
        this.processors = new LinkedList<>();
        this.importProcessors = new LinkedList<>();
        this.processor = null;
        this.importProcessor = null;
    }

    @Override
    public void addPreprocessor(ShaderPreProcessor processor, boolean modifyImports) {
        this.processors.add(processor);
        this.processor = null;
        if (modifyImports) {
            this.importProcessors.add(processor);
            this.importProcessor = null;
        }
    }

    public ShaderPreProcessor getProcessor() {
        if (this.processor == null) {
            this.processor = ShaderPreProcessor.allOf(this.processors);
        }
        return this.processor;
    }

    public ShaderPreProcessor getImportProcessor() {
        if (this.importProcessor == null) {
            this.importProcessor = ShaderPreProcessor.allOf(this.importProcessors);
        }
        return this.importProcessor;
    }
}
