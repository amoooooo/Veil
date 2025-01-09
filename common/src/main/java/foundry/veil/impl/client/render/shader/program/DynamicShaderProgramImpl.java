package foundry.veil.impl.client.render.shader.program;

import foundry.veil.Veil;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.*;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderProcessorList;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import foundry.veil.api.glsl.GlslParser;
import foundry.veil.api.glsl.GlslSyntaxException;
import foundry.veil.api.glsl.node.GlslTree;
import foundry.veil.lib.anarres.cpp.LexerException;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

@ApiStatus.Internal
public class DynamicShaderProgramImpl extends ShaderProgramImpl {

    private final Runnable onFree;
    private final Int2ObjectMap<String> shaderSources;
    private final Int2ObjectMap<VeilShaderSource> processedShaderSources;
    private ShaderProgramImpl oldShader;

    public DynamicShaderProgramImpl(ResourceLocation id, Runnable onFree) {
        super(id);
        this.onFree = onFree;
        this.shaderSources = new Int2ObjectArrayMap<>();
        this.processedShaderSources = new Int2ObjectArrayMap<>();
        this.oldShader = null;
    }

    @Override
    protected void attachShaders(CompiledProgram compiledProgram, ShaderSourceSet sourceSet, ShaderCompiler compiler) throws ShaderException {
        for (Int2ObjectMap.Entry<VeilShaderSource> entry : this.processedShaderSources.int2ObjectEntrySet()) {
            int glType = entry.getIntKey();
            VeilShaderSource source = entry.getValue();
            compiledProgram.attachShader(glType, compiler.compile(glType, ProgramDefinition.SourceType.GLSL, source));
        }

        // Fragment shaders aren't strictly necessary if the fragment output isn't used,
        // however mac shaders don't work without a fragment shader. This adds a "dummy" fragment shader
        // on mac specifically for all rendering shaders.
        if (Minecraft.ON_OSX && !this.processedShaderSources.containsKey(GL_COMPUTE_SHADER) && !this.processedShaderSources.containsKey(GL_FRAGMENT_SHADER)) {
            compiledProgram.attachShader(GL_FRAGMENT_SHADER, compiler.compile(GL_FRAGMENT_SHADER, ProgramDefinition.SourceType.GLSL, DUMMY_FRAGMENT_SHADER));
        }
    }

    public void setShaderSources(Int2ObjectMap<String> shaderSources) {
        this.shaderSources.clear();
        this.shaderSources.putAll(shaderSources);
    }

    public void processShaderSources(ShaderProcessorList processorList, ShaderPreDefinitions definitions, int activeBuffers) {
        this.processedShaderSources.clear();

        ShaderPreProcessor processor = processorList.getProcessor();
        ShaderPreProcessor importProcessor = processorList.getImportProcessor();
        Map<String, Object> customProgramData = new HashMap<>();

        try {
            for (Int2ObjectMap.Entry<String> shader : this.shaderSources.int2ObjectEntrySet()) {
                int type = shader.getIntKey();

                try {
                    String source = shader.getValue();

                    processor.prepare();
                    importProcessor.prepare();

                    Map<String, String> macros = new HashMap<>(definitions.getStaticDefinitions());
                    DynamicBufferType.addMacros(activeBuffers, macros);
                    GlslTree tree = GlslParser.preprocessParse(source, macros);
                    Object2IntMap<String> uniformBindings = new Object2IntArrayMap<>();
                    PreProcessorContext preProcessorContext = new PreProcessorContext(customProgramData, processorList, activeBuffers, type, uniformBindings, macros, null, true);
                    processor.modify(preProcessorContext, tree);
                    GlslTree.stripGLMacros(macros);
                    tree.getMacros().putAll(macros);

                    this.processedShaderSources.put(type, new VeilShaderSource(null, tree.toSourceString(), uniformBindings, Collections.emptySet(), new HashSet<>(processorList.getShaderImporter().addedImports())));
                } catch (Throwable t) {
                    throw new IOException("Failed to process " + ShaderManager.getTypeName(type) + " shader", t);
                }
            }
        } catch (IOException e) {
            this.processedShaderSources.clear();
            Veil.LOGGER.error("Couldn't parse dynamic shader: {}", this.getName(), e);
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

    private record PreProcessorContext(Map<String, Object> customProgramData,
                                       ShaderProcessorList processor,
                                       int activeBuffers,
                                       int type,
                                       Object2IntMap<String> uniformBindings,
                                       Map<String, String> macros,
                                       @Nullable ResourceLocation name,
                                       boolean sourceFile) implements ShaderPreProcessor.VeilContext {

        @Override
        public GlslTree modifyInclude(@Nullable ResourceLocation name, String source) throws IOException, GlslSyntaxException, LexerException {
            GlslTree tree = GlslParser.preprocessParse(source, this.macros);
            PreProcessorContext context = new PreProcessorContext(this.customProgramData, this.processor, this.activeBuffers, this.type, this.uniformBindings, this.macros, name, false);
            this.processor.getImportProcessor().modify(context, tree);
            return tree;
        }

        @Override
        public void addUniformBinding(String name, int binding) {
            this.uniformBindings.put(name, binding);
        }

        @Override
        public @Nullable ProgramDefinition definition() {
            return null;
        }

        @Override
        public boolean isSourceFile() {
            return this.sourceFile;
        }

        @Override
        public ShaderImporter shaderImporter() {
            return this.processor.getShaderImporter();
        }
    }
}
