package foundry.veil.impl.client.render.shader;

import foundry.veil.Veil;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.*;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferProcessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.glCreateProgram;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

@ApiStatus.Internal
public class DynamicShaderProgramImpl extends ShaderProgramImpl {

    private final Runnable onFree;
    private final Int2ObjectMap<String> shaderSources;
    private final Int2ObjectMap<VeilShaderSource> processedShaderSources;
    private ShaderProgramImpl oldShader;

    public DynamicShaderProgramImpl(ResourceLocation id, ShaderProgramImpl oldShader, Runnable onFree) {
        super(id);
        this.onFree = onFree;
        this.shaderSources = new Int2ObjectArrayMap<>();
        this.processedShaderSources = new Int2ObjectArrayMap<>();
        this.oldShader = oldShader;
    }

    @Override
    protected void compileInternal(int activeBuffers, ShaderSourceSet sourceSet, ShaderCompiler compiler) throws ShaderException {
        if (this.program == 0) {
            this.program = glCreateProgram();
        }

        try {
            for (Int2ObjectMap.Entry<VeilShaderSource> entry : this.processedShaderSources.int2ObjectEntrySet()) {
                int glType = entry.getIntKey();
                VeilShaderSource source = entry.getValue();
                this.attachShader(glType, compiler.compile(glType, ProgramDefinition.SourceType.GLSL, source), activeBuffers);
            }

            // Fragment shaders aren't strictly necessary if the fragment output isn't used,
            // however mac shaders don't work without a fragment shader. This adds a "dummy" fragment shader
            // on mac specifically for all rendering shaders.
            if (Minecraft.ON_OSX && !this.processedShaderSources.containsKey(GL_COMPUTE_SHADER) && !this.processedShaderSources.containsKey(GL_FRAGMENT_SHADER)) {
                this.attachShader(GL_FRAGMENT_SHADER, compiler.compile(GL_FRAGMENT_SHADER, ProgramDefinition.SourceType.GLSL, DUMMY_FRAGMENT_SHADER), activeBuffers);
            }

            this.link();
        } catch (Exception e) {
            this.freeInternal(); // F
            throw e;
        }
    }

    public void setShaderSources(Int2ObjectMap<String> shaderSources) {
        this.shaderSources.clear();
        this.shaderSources.putAll(shaderSources);
    }

    public void processShaderSources(ShaderProcessorList processorList, ShaderPreDefinitions definitions, int flags) {
        this.processedShaderSources.clear();

        ShaderPreProcessor processor = processorList.getProcessor();
        ShaderPreProcessor importProcessor = processorList.getImportProcessor();

        try {
            for (Int2ObjectMap.Entry<String> shader : this.shaderSources.int2ObjectEntrySet()) {
                int type = shader.getIntKey();

                try {
                    String source = shader.getValue();

                    processor.prepare();
                    importProcessor.prepare();

                    Object2IntMap<String> uniformBindings = new Object2IntArrayMap<>();
                    Set<String> dependencies = new HashSet<>();
                    Set<ResourceLocation> includes = new HashSet<>();
                    PreProcessorContext preProcessorContext = new PreProcessorContext(importProcessor, definitions, type, uniformBindings, dependencies, includes, null, true);
                    String transformed = processor.modify(preProcessorContext, source);

                    if (flags != 0) {
                        DynamicBufferType[] types = DynamicBufferType.decode(flags);
                        DynamicBufferProcessor bufferProcessor = new DynamicBufferProcessor(types);
                        transformed = bufferProcessor.modify(preProcessorContext, transformed);
                    }

                    this.processedShaderSources.put(type, new VeilShaderSource(null, transformed, uniformBindings, dependencies, includes));
                } catch (Throwable t) {
                    throw new IOException("Failed to process " + ShaderManager.getTypeName(type) + " shader", t);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            this.processedShaderSources.clear();
            Veil.LOGGER.error("Couldn't parse dynamic shader: {}", this.getId(), e);
        }
    }

    @Override
    public void free() {
        super.free();
        this.onFree.run();
    }

    public @Nullable ShaderProgramImpl getOldShader() {
        return this.oldShader;
    }

    public void setOldShader(@Nullable ShaderProgramImpl program) {
        this.oldShader = program;
    }

    private record PreProcessorContext(ShaderPreProcessor importProcessor,
                                       ShaderPreDefinitions preDefinitions,
                                       int type,
                                       Map<String, Integer> uniformBindings,
                                       Set<String> dependencies,
                                       Set<ResourceLocation> includes,
                                       @Nullable ResourceLocation name,
                                       boolean sourceFile) implements ShaderPreProcessor.VeilContext {

        @Override
        public String modify(@Nullable ResourceLocation name, String source) throws IOException {
            PreProcessorContext context = new PreProcessorContext(this.importProcessor, this.preDefinitions, this.type, this.uniformBindings, this.dependencies, this.includes, name, false);
            return this.importProcessor.modify(context, source);
        }

        @Override
        public void addUniformBinding(String name, int binding) {
            this.uniformBindings.put(name, binding);
        }

        @Override
        public void addPreDefinitionDependency(String name) {
            this.dependencies.add(name);
        }

        @Override
        public void addInclude(ResourceLocation name) {
            this.includes.add(name);
        }

        @Override
        public @Nullable ProgramDefinition definition() {
            return null;
        }

        @Override
        public boolean isSourceFile() {
            return this.sourceFile;
        }
    }
}
