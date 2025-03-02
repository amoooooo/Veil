package foundry.veil.impl.client.render.shader.processor;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.event.VeilAddShaderPreProcessorsEvent;
import foundry.veil.impl.client.render.shader.ShaderImporterImpl;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedList;
import java.util.List;

@ApiStatus.Internal
public class ShaderProcessorList implements VeilAddShaderPreProcessorsEvent.Registry {

    private final List<ShaderPreProcessor> processors;
    private final List<ShaderPreProcessor> importProcessors;
    private final ShaderImporterImpl importer;
    private ShaderPreProcessor processor;
    private ShaderPreProcessor importProcessor;

    public ShaderProcessorList(ResourceProvider provider) {
        this.processors = new LinkedList<>();
        this.importProcessors = new LinkedList<>();
        this.importer = new ShaderImporterImpl(provider);
        this.processor = null;
        this.importProcessor = null;
    }

    @Override
    public void addPreprocessorFirst(ShaderPreProcessor processor, boolean modifyImports) {
        this.processors.addFirst(processor);
        this.processor = null;
        if (modifyImports) {
            this.importProcessors.addFirst(processor);
            this.importProcessor = null;
        }
    }

    @Override
    public void addPreprocessor(ShaderPreProcessor processor, boolean modifyImports) {
        this.processors.addLast(processor);
        this.processor = null;
        if (modifyImports) {
            this.importProcessors.addLast(processor);
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

    public ShaderImporterImpl getShaderImporter() {
        return this.importer;
    }
}
