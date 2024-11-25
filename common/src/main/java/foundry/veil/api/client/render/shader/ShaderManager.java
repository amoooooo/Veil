package foundry.veil.api.client.render.shader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.processor.*;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.ThreadTaskScheduler;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManger;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferProcessor;
import foundry.veil.impl.client.render.shader.ShaderProgramImpl;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_EVALUATION_SHADER;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

/**
 * Manages all shaders and compiles them automatically.
 *
 * @author Ocelot
 * @see ShaderCompiler
 */
public class ShaderManager implements PreparableReloadListener, Closeable {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(ProgramDefinition.class, new ProgramDefinition.Deserializer())
            .create();

    public static final FileToIdConverter INCLUDE_LISTER = new FileToIdConverter("pinwheel/shaders/include", ".glsl");
    public static final ShaderSourceSet PROGRAM_SET = new ShaderSourceSet("pinwheel/shaders/program");
    public static final ShaderSourceSet DEFERRED_SET = new ShaderSourceSet("pinwheel/shaders/deferred");

    private static final Map<Integer, String> TYPES = Map.of(
            GL_VERTEX_SHADER, "vertex",
            GL_TESS_CONTROL_SHADER, "tesselation_control",
            GL_TESS_EVALUATION_SHADER, "tesselation_evaluation",
            GL_GEOMETRY_SHADER, "geometry",
            GL_FRAGMENT_SHADER, "fragment",
            GL_COMPUTE_SHADER, "compute"
    );

    private final DynamicBufferManger dynamicBufferManager;
    private final ShaderSourceSet sourceSet;
    private final ShaderPreDefinitions definitions;
    private final Map<ResourceLocation, ShaderProgram> shaders;
    private final Map<ResourceLocation, ShaderProgram> shadersView;
    private final Set<ResourceLocation> dirtyShaders;

    private CompletableFuture<Void> reloadFuture;
    private CompletableFuture<Void> recompileFuture;
    private CompletableFuture<Void> updateBuffersFuture;

    /**
     * Creates a new shader manager.
     *
     * @param sourceSet            The source set to load all shaders from
     * @param shaderPreDefinitions The set of shader pre-definitions
     * @param dynamicBufferManager The manager for dynamic buffers
     */
    public ShaderManager(ShaderSourceSet sourceSet, ShaderPreDefinitions shaderPreDefinitions, DynamicBufferManger dynamicBufferManager) {
        this.dynamicBufferManager = dynamicBufferManager;
        this.sourceSet = sourceSet;
        this.definitions = shaderPreDefinitions;
        this.definitions.addListener(this::onDefinitionChanged);
        this.shaders = new HashMap<>();
        this.shadersView = Collections.unmodifiableMap(this.shaders);
        this.dirtyShaders = new HashSet<>();

        this.reloadFuture = CompletableFuture.completedFuture(null);
        this.recompileFuture = CompletableFuture.completedFuture(null);
        this.updateBuffersFuture = CompletableFuture.completedFuture(null);
    }

    private void onDefinitionChanged(String definition) {
        this.shaders.values().forEach(shader -> {
            if (shader.getDefinitionDependencies().contains(definition)) {
                Veil.LOGGER.debug("{} changed, recompiling {}", definition, shader.getId());
                this.scheduleRecompile(shader.getId());
            }
        });
    }

    private void addProcessors(ShaderProcessorList processorList, ResourceProvider provider) {
        if (provider != null) {
            processorList.addPreprocessor(new ShaderImportProcessor(provider));
        }
        processorList.addPreprocessor(new ShaderBindingProcessor());
        processorList.addPreprocessor(new ShaderPredefinitionProcessor(), false);
        processorList.addPreprocessor(new ShaderVersionProcessor(), false);
        processorList.addPreprocessor(new ShaderModifyProcessor(), false);
        processorList.addPreprocessor(new ShaderCustomProcessor(provider), false);
        // TODO add event to add custom processors
    }

    private ProgramDefinition parseDefinition(ResourceLocation id, ResourceProvider provider) throws IOException {
        try (Reader reader = provider.openAsReader(this.sourceSet.getShaderDefinitionLister().idToFile(id))) {
            ProgramDefinition definition = GsonHelper.fromJson(GSON, reader, ProgramDefinition.class);
            if (definition.vertex() == null &&
                    definition.tesselationControl() == null &&
                    definition.tesselationEvaluation() == null &&
                    definition.geometry() == null &&
                    definition.fragment() == null &&
                    definition.compute() == null) {
                throw new JsonSyntaxException("Shader programs must define at least 1 shader type");
            }

            return definition;
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    private void readShader(ShaderProcessorList processorList, ResourceProvider resourceProvider, Map<ResourceLocation, ProgramDefinition> definitions, Map<ResourceLocation, VeilShaderSource> shaderSources, ResourceLocation definitionId, int flags) {
        Set<ResourceLocation> checkedSources = new HashSet<>();

        ShaderPreProcessor processor = processorList.getProcessor();
        ShaderPreProcessor importProcessor = processorList.getImportProcessor();

        try {
            ProgramDefinition definition = this.parseDefinition(definitionId, resourceProvider);
            if (definitions.put(definitionId, definition) != null) {
                throw new IllegalStateException("Duplicate shader ignored with ID " + definitionId);
            }

            for (Int2ObjectMap.Entry<ProgramDefinition.ShaderSource> shader : definition.shaders().int2ObjectEntrySet()) {
                int type = shader.getIntKey();
                ResourceLocation shaderId = shader.getValue().location();

                FileToIdConverter typeConverter = this.sourceSet.getTypeConverter(type);
                ResourceLocation location = typeConverter.idToFile(shaderId);

                if (!checkedSources.add(location)) {
                    continue;
                }

                Resource resource = resourceProvider.getResourceOrThrow(location);
                try (Reader reader = resource.openAsReader()) {
                    String source = IOUtils.toString(reader);

                    processor.prepare();
                    importProcessor.prepare();

                    Object2IntMap<String> uniformBindings = new Object2IntArrayMap<>();
                    Set<String> dependencies = new HashSet<>();
                    Set<ResourceLocation> includes = new HashSet<>();
                    PreProcessorContext preProcessorContext = new PreProcessorContext(importProcessor, definition, this.definitions, type, uniformBindings, dependencies, includes, shaderId, true);
                    String transformed = processor.modify(preProcessorContext, source);

                    if (flags != 0) {
                        DynamicBufferType[] types = DynamicBufferType.decode(flags);
                        DynamicBufferProcessor bufferProcessor = new DynamicBufferProcessor(types);
                        transformed = bufferProcessor.modify(preProcessorContext, transformed);
                    }

                    shaderSources.put(location, new VeilShaderSource(shaderId, transformed, uniformBindings, dependencies, includes));
                } catch (Throwable t) {
                    throw new IOException("Failed to load " + getTypeName(type) + " shader", t);
                }
            }
        } catch (IOException | IllegalArgumentException | JsonParseException e) {
            Veil.LOGGER.error("Couldn't parse shader {} from {}", definitionId, this.sourceSet.getShaderDefinitionLister().idToFile(definitionId), e);
        }
    }

//    private Map<ResourceLocation, Resource> readIncludes(ResourceManager resourceManager) {
//        Map<ResourceLocation, Resource> shaderSources = new HashMap<>();
//        Set<ResourceLocation> checkedSources = new HashSet<>();
//
//        for (Map.Entry<ResourceLocation, Resource> entry : INCLUDE_LISTER.listMatchingResources(resourceManager).entrySet()) {
//            ResourceLocation location = entry.getKey();
//            ResourceLocation id = INCLUDE_LISTER.fileToId(location);
//
//            if (!checkedSources.add(location)) {
//                continue;
//            }
//
//            try {
//                Resource resource = resourceManager.getResourceOrThrow(location);
//                try (InputStream stream = resource.open()) {
//                    byte[] source = stream.readAllBytes();
//                    Resource fileResource = new Resource(resource.source(), () -> new ByteArrayInputStream(source));
//                    shaderSources.put(location, fileResource);
//                }
//            } catch (IOException | IllegalArgumentException | JsonParseException e) {
//                Veil.LOGGER.error("Couldn't parse shader import {} from {}", id, location, e);
//            }
//        }
//
//        return shaderSources;
//    }

    private void compile(ShaderProgram program, ProgramDefinition definition, ShaderCompiler compiler) {
        ResourceLocation id = program.getId();
        try {
            program.compile(new ShaderCompiler.Context(this.dynamicBufferManager.getActiveBuffers(), this.sourceSet, definition), compiler);
        } catch (ShaderException e) {
            Veil.LOGGER.error("Failed to create shader {}: {}", id, e.getMessage());
            String error = e.getGlError();
            if (error != null) {
                Veil.LOGGER.warn(error);
            }
        } catch (Exception e) {
            Veil.LOGGER.error("Failed to create shader: {}", id, e);
        }
    }

    /**
     * Sets a global shader value.
     *
     * @param setter The setter for shaders
     */
    public void setGlobal(Consumer<ShaderProgram> setter) {
        this.shaders.values().forEach(setter);
    }

    /**
     * Retrieves a shader by the specified id.
     *
     * @param id The id of the shader to retrieve
     * @return The retrieved shader or <code>null</code> if there is no valid shader with that id
     */
    public @Nullable ShaderProgram getShader(ResourceLocation id) {
        return this.shaders.get(id);
    }

    /**
     * @return All shader programs registered
     */
    public Map<ResourceLocation, ShaderProgram> getShaders() {
        return this.shadersView;
    }

    /**
     * @return The source set all shaders are loaded from
     */
    public ShaderSourceSet getSourceSet() {
        return this.sourceSet;
    }

    private CompletableFuture<ReloadState> prepare(ResourceManager resourceManager, Collection<ResourceLocation> shaders, int activeBuffers, Executor executor) {
        Map<ResourceLocation, ProgramDefinition> definitions = new ConcurrentHashMap<>();
        Map<ResourceLocation, VeilShaderSource> shaderSources = new ConcurrentHashMap<>();

        Long2ObjectMap<ShaderProcessorList> processorList = Long2ObjectMaps.synchronize(new Long2ObjectArrayMap<>());
        ThreadTaskScheduler scheduler = new ThreadTaskScheduler("VeilShaderCompiler", Math.max(1, Runtime.getRuntime().availableProcessors() / 4), () -> {
            Iterator<ResourceLocation> iterator = shaders.iterator();
            if (iterator.hasNext()) {
                ResourceLocation key = iterator.next();
                iterator.remove();
                return () -> {
                    ShaderProcessorList shaderProcessor = processorList.computeIfAbsent(Thread.currentThread().threadId(), id -> {
                        ShaderProcessorList list = new ShaderProcessorList();
                        this.addProcessors(list, resourceManager);
                        return list;
                    });
                    this.readShader(shaderProcessor, resourceManager, definitions, shaderSources, key, activeBuffers);
                };
            }
            return null;
        });

        return scheduler.getCompletedFuture().thenApplyAsync(unused -> new ReloadState(definitions, shaderSources), executor);
    }

    private void apply(ShaderManager.ReloadState reloadState) {
        this.shaders.values().forEach(ShaderProgram::free);
        this.shaders.clear();

        try (ShaderCompiler compiler = ShaderCompiler.direct(reloadState.shaderSources::get)) {
            for (Map.Entry<ResourceLocation, ProgramDefinition> entry : reloadState.definitions().entrySet()) {
                ResourceLocation id = entry.getKey();
                ShaderProgram program = ShaderProgram.create(id);
                this.compile(program, entry.getValue(), compiler);
                this.shaders.put(id, program);
            }
        }

        VeilRenderSystem.finalizeShaderCompilation();

        Veil.LOGGER.info("Loaded {} shaders from: {}", this.shaders.size(), this.sourceSet.getFolder());
    }

    private void applyRecompile(ShaderManager.ReloadState reloadState, int shaderCount) {
        try (ShaderCompiler compiler = ShaderCompiler.direct(reloadState.shaderSources::get)) {
            for (Map.Entry<ResourceLocation, ProgramDefinition> entry : reloadState.definitions().entrySet()) {
                ResourceLocation id = entry.getKey();
                ShaderProgram program = this.getShader(id);
                if (program == null) {
                    Veil.LOGGER.warn("Failed to recompile shader: {}", id);
                    continue;
                }
                this.compile(program, entry.getValue(), compiler);
            }
        }

        VeilRenderSystem.finalizeShaderCompilation();

        Veil.LOGGER.info("Recompiled {} shaders from: {}", shaderCount, this.sourceSet.getFolder());
    }

    private void scheduleRecompile(int attempt) {
        Minecraft client = Minecraft.getInstance();
        client.tell(() -> {
            if (!this.recompileFuture.isDone()) {
                return;
            }

            Set<ResourceLocation> shaders;
            synchronized (this.dirtyShaders) {
                shaders = ConcurrentHashMap.newKeySet(this.dirtyShaders.size());
                shaders.addAll(this.dirtyShaders);
                this.dirtyShaders.clear();
            }
            int shaderCount = shaders.size();
            int activeBuffers = this.dynamicBufferManager.getActiveBuffers();
            this.recompileFuture = this.prepare(client.getResourceManager(), shaders, activeBuffers, Util.backgroundExecutor())
                    .thenAcceptAsync(state -> this.applyRecompile(state, shaderCount), client)
                    .handle((value, e) -> {
                        if (e != null) {
                            Veil.LOGGER.error("Error recompiling shaders", e);
                        }

                        synchronized (this.dirtyShaders) {
                            if (this.dirtyShaders.isEmpty()) {
                                return value;
                            }
                        }

                        if (attempt >= 3) {
                            Veil.LOGGER.error("Failed to recompile shaders after {} attempts", attempt);
                            return value;
                        }

                        this.scheduleRecompile(attempt + 1);
                        return value;
                    });
        });
    }

    /**
     * Schedules a shader recompilation on the next loop iteration.
     *
     * @param shader The shader to recompile
     */
    public void scheduleRecompile(ResourceLocation shader) {
        synchronized (this.dirtyShaders) {
            this.dirtyShaders.add(shader);
        }

        if (!this.recompileFuture.isDone()) {
            return;
        }

        this.scheduleRecompile(0);
    }

    @ApiStatus.Internal
    public void setActiveBuffers(int activeBuffers) {
        ShaderProgram active = null;

        try {
            Set<ResourceLocation> shaders = ConcurrentHashMap.newKeySet(this.shaders.size());
            for (ShaderProgram program : this.shaders.values()) {
                active = program;
                if (program instanceof ShaderProgramImpl impl) {
                    if (impl.setActiveBuffers(activeBuffers)) {
                        shaders.add(program.getId());
                    }
                }
            }

            if (!shaders.isEmpty()) {
                int shaderCount = shaders.size();
                this.updateBuffersFuture = this.updateBuffersFuture
                        .thenComposeAsync(unused -> this.prepare(Minecraft.getInstance().getResourceManager(), shaders, activeBuffers, Util.backgroundExecutor()), Util.backgroundExecutor())
                        .thenAcceptAsync(reloadState -> {
                            try (ShaderCompiler compiler = ShaderCompiler.direct(reloadState.shaderSources::get)) {
                                for (Map.Entry<ResourceLocation, ProgramDefinition> entry : reloadState.definitions().entrySet()) {
                                    ResourceLocation id = entry.getKey();
                                    ShaderProgram program = this.getShader(id);
                                    if (!(program instanceof ShaderProgramImpl impl)) {
                                        Veil.LOGGER.warn("Failed to set shader active buffers: {}", id);
                                        continue;
                                    }

                                    try {
                                        impl.updateActiveBuffers(new ShaderCompiler.Context(activeBuffers, this.sourceSet, null), compiler);
                                    } catch (ShaderException e) {
                                        Veil.LOGGER.error("Failed to create shader {}: {}", id, e.getMessage());
                                        String error = e.getGlError();
                                        if (error != null) {
                                            Veil.LOGGER.warn(error);
                                        }
                                    } catch (Exception e) {
                                        Veil.LOGGER.error("Failed to create shader: {}", id, e);
                                    }
                                }
                            }

                            VeilRenderSystem.finalizeShaderCompilation();

                            Veil.LOGGER.info("Compiled {} shaders from: {}", shaderCount, this.sourceSet.getFolder());
                        }, Minecraft.getInstance());
            }
        } catch (ShaderException e) {
            Veil.LOGGER.error("Failed to set shader active buffers {}: {}", active.getId(), e.getMessage());
            String error = e.getGlError();
            if (error != null) {
                Veil.LOGGER.warn(error);
            }
        }
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        if (this.reloadFuture != null && !this.reloadFuture.isDone()) {
            return this.reloadFuture.thenCompose(preparationBarrier::wait);
        }
        int activeBuffers = this.dynamicBufferManager.getActiveBuffers();
        return this.reloadFuture = CompletableFuture.allOf(this.recompileFuture, this.updateBuffersFuture).thenCompose(
                unused -> CompletableFuture.supplyAsync(() -> {
                            FileToIdConverter lister = this.sourceSet.getShaderDefinitionLister();
                            Set<ResourceLocation> shaderIds = lister.listMatchingResources(resourceManager).keySet()
                                    .stream()
                                    .map(lister::fileToId)
                                    .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
                            return this.prepare(resourceManager, shaderIds, activeBuffers, backgroundExecutor);
                        }, backgroundExecutor)
                        .thenCompose(future -> future)
                        .thenCompose(preparationBarrier::wait)
                        .thenAcceptAsync(this::apply, gameExecutor));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName() + " " + this.getSourceSet().getFolder();
    }

    /**
     * @return The current future for full shader reload status
     */
    public CompletableFuture<Void> getReloadFuture() {
        return this.reloadFuture;
    }

    /**
     * @return The current future for dirty shader recompilation status
     */
    public CompletableFuture<Void> getRecompileFuture() {
        return this.recompileFuture;
    }

    /**
     * Retrieves a readable name for a shader type. Supports all shader types instead of just vertex and fragment.
     *
     * @param type The GL enum for the type
     * @return The readable name or a hex value if the type is unknown
     */
    public static String getTypeName(int type) {
        String value = TYPES.get(type);
        return value != null ? value : "0x" + Integer.toHexString(type);
    }

    @Override
    public void close() {
        this.shaders.values().forEach(ShaderProgram::free);
        this.shaders.clear();
    }

    private record PreProcessorContext(ShaderPreProcessor preProcessor,
                                       @Nullable ProgramDefinition definition,
                                       ShaderPreDefinitions preDefinitions,
                                       int type,
                                       Map<String, Integer> uniformBindings,
                                       Set<String> dependencies,
                                       Set<ResourceLocation> includes,
                                       @Nullable ResourceLocation name,
                                       boolean sourceFile) implements ShaderPreProcessor.Context {

        @Override
        public String modify(@Nullable ResourceLocation name, String source) throws IOException {
            PreProcessorContext context = new PreProcessorContext(this.preProcessor, this.definition, this.preDefinitions, this.type, this.uniformBindings, this.dependencies, this.includes, name, false);
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
        public boolean isSourceFile() {
            return this.sourceFile;
        }

        @Override
        public @Nullable String shaderInstance() {
            return null;
        }

        @Override
        public @Nullable VertexFormat vertexFormat() {
            return null;
        }
    }

    private record ReloadState(Map<ResourceLocation, ProgramDefinition> definitions,
                               Map<ResourceLocation, VeilShaderSource> shaderSources) {
    }

    private static class ShaderProcessorList {

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

        /**
         * Adds the specified pre-processor to the end of the stack.
         *
         * @param processor     The processor to add
         * @param modifyImports Whether the processor will also be run on imports
         */
        public void addPreprocessor(ShaderPreProcessor processor, boolean modifyImports) {
            this.processors.add(processor);
            this.processor = null;
            if (modifyImports) {
                this.importProcessors.add(processor);
                this.importProcessor = null;
            }
        }

        /**
         * Adds the specified pre-processor to the end of the stack.
         *
         * @param processor The processor to add
         */
        public void addPreprocessor(ShaderPreProcessor processor) {
            this.addPreprocessor(processor, true);
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
}
