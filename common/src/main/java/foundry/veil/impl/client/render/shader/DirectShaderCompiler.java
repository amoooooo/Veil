package foundry.veil.impl.client.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.CompiledShader;
import foundry.veil.api.client.render.shader.ShaderCompiler;
import foundry.veil.api.client.render.shader.ShaderException;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.processor.*;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20C;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

/**
 * Creates a new shader and compiles each time {@link #compile(ShaderCompiler.Context, int, String)} is called.
 * This should only be used for compiling single shaders.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class DirectShaderCompiler implements ShaderCompiler {

    private final ResourceProvider provider;
    private final List<ShaderPreProcessor> processors;
    private final List<ShaderPreProcessor> importProcessors;
    private final Set<Integer> shaders;
    private ResourceLocation compilingName;
    private ShaderPreProcessor processor;
    private ShaderPreProcessor importProcessor;

    public DirectShaderCompiler(@Nullable ResourceProvider provider) {
        this.provider = provider;
        this.processors = new LinkedList<>();
        this.importProcessors = new LinkedList<>();
        this.shaders = new HashSet<>();
    }

    private ShaderPreProcessor getProcessor() {
        if (this.processor == null) {
            this.processor = ShaderPreProcessor.allOf(this.processors);
        }
        return this.processor;
    }

    private ShaderPreProcessor getImportProcessor() {
        if (this.importProcessor == null) {
            this.importProcessor = ShaderPreProcessor.allOf(this.importProcessors);
        }
        return this.importProcessor;
    }

    private void validateType(int type) throws ShaderException {
        if (type == GL_COMPUTE_SHADER && !VeilRenderSystem.computeSupported()) {
            throw new ShaderException("Compute is not supported", null);
        }
    }

    @Override
    public CompiledShader compile(ShaderCompiler.Context context, int type, ResourceLocation id) throws IOException, ShaderException {
        if (this.provider == null) {
            throw new IOException("Failed to read " + ShaderManager.getTypeName(type) + " from " + id + " because no provider was specified");
        }
        this.validateType(type);

        ResourceLocation location = context.sourceSet().getTypeConverter(type).idToFile(id);
        try (Reader reader = this.provider.openAsReader(location)) {
            this.compilingName = id;
            return this.compile(context, type, IOUtils.toString(reader));
        } finally {
            this.compilingName = null;
        }
    }

    @Override
    public CompiledShader compile(ShaderCompiler.Context context, int type, String source) throws IOException, ShaderException {
        this.validateType(type);
        ShaderPreProcessor processor = this.getProcessor();
        ShaderPreProcessor importProcessor = this.getImportProcessor();
        processor.prepare();
        importProcessor.prepare();

        Object2IntMap<String> uniformBindings = new Object2IntArrayMap<>();
        Set<String> dependencies = new HashSet<>();
        Set<ResourceLocation> includes = new HashSet<>();
        Set<ResourceLocation> includesView = Collections.unmodifiableSet(includes);
        String transformed = processor.modify(new PreProcessorContext(importProcessor, context, uniformBindings, dependencies, includes, includesView, this.compilingName, true), source);

        int shader = glCreateShader(type);
        GlStateManager.glShaderSource(shader, List.of(transformed));
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
            String log = glGetShaderInfoLog(shader);
            if (Veil.VERBOSE_SHADER_ERRORS) {
                log += "\n" + transformed;
            }
            glDeleteShader(shader); // Delete to prevent leaks
            throw new ShaderException("Failed to compile " + ShaderManager.getTypeName(type) + " shader", log);
        }

        this.shaders.add(shader);
        return new CompiledShader(this.compilingName, shader, Object2IntMaps.unmodifiable(uniformBindings), Collections.unmodifiableSet(dependencies), includesView);
    }

    @Override
    public ShaderCompiler addPreprocessor(ShaderPreProcessor processor, boolean modifyImports) {
        this.processors.add(processor);
        this.processor = null;
        if (modifyImports) {
            this.importProcessors.add(processor);
            this.importProcessor = null;
        }
        return this;
    }

    @Override
    public ShaderCompiler addDefaultProcessors() {
        if (this.provider != null) {
            this.addPreprocessor(new ShaderImportProcessor(this.provider));
        }
        this.addPreprocessor(new ShaderBindingProcessor());
        this.addPreprocessor(new ShaderPredefinitionProcessor(), false);
        this.addPreprocessor(new ShaderVersionProcessor(), false);
        return this;
    }

    @Override
    public void free() {
        this.processors.clear();
        this.importProcessors.clear();
        this.shaders.forEach(GL20C::glDeleteShader);
        this.shaders.clear();
    }

    private record PreProcessorContext(ShaderPreProcessor preProcessor,
                                       ShaderCompiler.Context context,
                                       Map<String, Integer> uniformBindings,
                                       Set<String> dependencies,
                                       Set<ResourceLocation> includes,
                                       Set<ResourceLocation> includesView,
                                       @Nullable ResourceLocation name,
                                       boolean sourceFile) implements ShaderPreProcessor.Context {

        @Override
        public String modify(@Nullable ResourceLocation name, String source) throws IOException {
            PreProcessorContext context = new PreProcessorContext(this.preProcessor, this.context, this.uniformBindings, this.dependencies, this.includes, this.includesView, name, false);
            return this.preProcessor.modify(context, source);
        }

        @Override
        public void addUniformBinding(String name, int binding) {
            this.uniformBindings.put(name, binding);
        }

        @Override
        public void addDefinitionDependency(String name) {
            this.dependencies.add(name);
        }

        @Override
        public void addInclude(ResourceLocation name) {
            this.includes.add(name);
        }

        @Override
        public Set<ResourceLocation> includes() {
            return this.includesView;
        }

        @Override
        public boolean isSourceFile() {
            return this.sourceFile;
        }

        @Override
        public @Nullable ProgramDefinition definition() {
            return this.context.definition();
        }

        @Override
        public ShaderPreDefinitions preDefinitions() {
            return this.context.preDefinitions();
        }
    }
}
