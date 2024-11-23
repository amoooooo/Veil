package foundry.veil.api.client.render.shader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.processor.ShaderCustomProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderModifyProcessor;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManger;
import foundry.veil.impl.client.render.shader.ShaderProgramImpl;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
 * <p>Manages all shaders and compiles them automatically.</p>
 * <p>Shaders can be recompiled using {@link #recompile(ResourceLocation, ResourceProvider)} or
 * {@link #recompile(ResourceLocation, ResourceProvider, ShaderCompiler)} to use a custom compiler.</p>
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

    private void readShader(ResourceManager resourceManager, Map<ResourceLocation, ProgramDefinition> definitions, Map<ResourceLocation, Resource> shaderSources, ResourceLocation id) {
        Set<ResourceLocation> checkedSources = new HashSet<>();

        try {
            ProgramDefinition definition = this.parseDefinition(id, resourceManager);
            if (definitions.put(id, definition) != null) {
                throw new IllegalStateException("Duplicate shader ignored with ID " + id);
            }

            for (Int2ObjectMap.Entry<ProgramDefinition.ShaderSource> shader : definition.shaders().int2ObjectEntrySet()) {
                FileToIdConverter typeConverter = this.sourceSet.getTypeConverter(shader.getIntKey());
                ResourceLocation location = typeConverter.idToFile(shader.getValue().location());

                if (!checkedSources.add(location)) {
                    continue;
                }

                Resource resource = resourceManager.getResourceOrThrow(location);
                try (InputStream stream = resource.open()) {
                    byte[] source = stream.readAllBytes();
                    Resource fileResource = new Resource(resource.source(), () -> new ByteArrayInputStream(source));
                    shaderSources.put(location, fileResource);
                } catch (Throwable t) {
                    throw new IOException("Failed to load " + getTypeName(shader.getIntKey()) + " shader", t);
                }
            }
        } catch (IOException | IllegalArgumentException | JsonParseException e) {
            Veil.LOGGER.error("Couldn't parse shader {} from {}", id, this.sourceSet.getShaderDefinitionLister().idToFile(id), e);
        }
    }

    private Map<ResourceLocation, Resource> readIncludes(ResourceManager resourceManager) {
        Map<ResourceLocation, Resource> shaderSources = new HashMap<>();
        Set<ResourceLocation> checkedSources = new HashSet<>();

        for (Map.Entry<ResourceLocation, Resource> entry : INCLUDE_LISTER.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation location = entry.getKey();
            ResourceLocation id = INCLUDE_LISTER.fileToId(location);

            if (!checkedSources.add(location)) {
                continue;
            }

            try {
                Resource resource = resourceManager.getResourceOrThrow(location);
                try (InputStream stream = resource.open()) {
                    byte[] source = stream.readAllBytes();
                    Resource fileResource = new Resource(resource.source(), () -> new ByteArrayInputStream(source));
                    shaderSources.put(location, fileResource);
                }
            } catch (IOException | IllegalArgumentException | JsonParseException e) {
                Veil.LOGGER.error("Couldn't parse shader import {} from {}", id, location, e);
            }
        }

        return shaderSources;
    }

    private void compile(ShaderProgram program, ProgramDefinition definition, ShaderCompiler compiler) {
        ResourceLocation id = program.getId();
        try {
            program.compile(new ShaderCompiler.Context(this.definitions, this.sourceSet, this.dynamicBufferManager.getActiveBuffers(), definition), compiler);
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

    private ShaderCompiler addProcessors(ShaderCompiler compiler, ResourceProvider provider) {
        return compiler.addDefaultProcessors()
                .addPreprocessor(new ShaderModifyProcessor(), false)
                .addPreprocessor(new ShaderCustomProcessor(provider), false);
    }

    /**
     * Attempts to recompile the shader with the specified id.
     *
     * @param id       The id of the shader to recompile
     * @param provider The source of resources
     */
    public void recompile(ResourceLocation id, ResourceProvider provider) {
        try (ShaderCompiler compiler = this.addProcessors(ShaderCompiler.direct(provider), provider)) {
            this.recompile(id, provider, compiler);
        }
    }

    /**
     * Attempts to recompile the shader with the specified id.
     *
     * @param id       The id of the shader to recompile
     * @param provider The source of resources
     * @param compiler The compiler instance to use. If unsure, use {@link #recompile(ResourceLocation, ResourceProvider)}
     */
    public void recompile(ResourceLocation id, ResourceProvider provider, ShaderCompiler compiler) {
        ShaderProgram program = this.shaders.get(id);
        if (program == null) {
            Veil.LOGGER.error("Failed to recompile unknown shader: {}", id);
            return;
        }

        try {
            this.compile(program, this.parseDefinition(id, provider), compiler);
        } catch (Exception e) {
            Veil.LOGGER.error("Failed to read shader definition: {}", id, e);
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

    private ReloadState prepare(ResourceManager resourceManager, Collection<ResourceLocation> shaders) {
        Map<ResourceLocation, ProgramDefinition> definitions = new HashMap<>();
        Map<ResourceLocation, Resource> shaderSources = new HashMap<>();

        for (ResourceLocation key : shaders) {
            this.readShader(resourceManager, definitions, shaderSources, key);
        }
        shaderSources.putAll(this.readIncludes(resourceManager));

        return new ReloadState(definitions, shaderSources);
    }

    private void apply(ShaderManager.ReloadState reloadState) {
        this.shaders.values().forEach(ShaderProgram::free);
        this.shaders.clear();

        ResourceProvider sourceProvider = loc -> Optional.ofNullable(reloadState.shaderSources().get(loc));
        try (ShaderCompiler compiler = this.addProcessors(ShaderCompiler.direct(sourceProvider), sourceProvider)) {
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

    private void applyRecompile(ShaderManager.ReloadState reloadState, Collection<ResourceLocation> shaders) {
        ResourceProvider sourceProvider = loc -> Optional.ofNullable(reloadState.shaderSources().get(loc));
        try (ShaderCompiler compiler = this.addProcessors(ShaderCompiler.direct(sourceProvider), sourceProvider)) {
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

        Veil.LOGGER.info("Recompiled {} shaders from: {}", shaders.size(), this.sourceSet.getFolder());
    }

    private void scheduleRecompile(int attempt) {
        Minecraft client = Minecraft.getInstance();
        client.tell(() -> {
            if (!this.recompileFuture.isDone()) {
                return;
            }

            Set<ResourceLocation> shaders;
            synchronized (this.dirtyShaders) {
                shaders = new HashSet<>(this.dirtyShaders);
                this.dirtyShaders.clear();
            }
            this.recompileFuture = CompletableFuture.supplyAsync(() -> this.prepare(client.getResourceManager(), shaders), Util.backgroundExecutor())
                    .thenAcceptAsync(state -> this.applyRecompile(state, shaders), client)
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
            Set<ResourceLocation> shaders = new HashSet<>();
            for (ShaderProgram program : this.shaders.values()) {
                active = program;
                if (program instanceof ShaderProgramImpl impl) {
                    if (impl.setActiveBuffers(activeBuffers)) {
                        shaders.add(program.getId());
                    }
                }
            }

            if (!shaders.isEmpty()) {
                this.updateBuffersFuture = this.updateBuffersFuture
                        .thenApplyAsync(unused -> this.prepare(Minecraft.getInstance().getResourceManager(), shaders), Util.backgroundExecutor())
                        .thenAcceptAsync(reloadState -> {
                            ResourceProvider sourceProvider = loc -> Optional.ofNullable(reloadState.shaderSources().get(loc));
                            try (ShaderCompiler compiler = this.addProcessors(ShaderCompiler.direct(sourceProvider), sourceProvider)) {
                                for (Map.Entry<ResourceLocation, ProgramDefinition> entry : reloadState.definitions().entrySet()) {
                                    ResourceLocation id = entry.getKey();
                                    ShaderProgram program = this.getShader(id);
                                    if (!(program instanceof ShaderProgramImpl impl)) {
                                        Veil.LOGGER.warn("Failed to set shader active buffers: {}", id);
                                        continue;
                                    }

                                    try {
                                        impl.updateActiveBuffers(new ShaderCompiler.Context(this.definitions, this.sourceSet, this.dynamicBufferManager.getActiveBuffers(), program.getDefinition()), compiler);
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

                            Veil.LOGGER.info("Compiled {} shaders from: {}", shaders.size(), this.sourceSet.getFolder());
                        }, Minecraft.getInstance());
            }
        } catch (ShaderException e) {
            CrashReport crashreport = CrashReport.forThrowable(e, "Setting Active Buffers");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Linking Program");
            crashreportcategory.setDetail("Name", active.getId());
            String glError = e.getGlError();
            if (glError != null) {
                Veil.LOGGER.error(glError);
            }
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        if (this.reloadFuture != null && !this.reloadFuture.isDone()) {
            return this.reloadFuture.thenCompose(preparationBarrier::wait);
        }
        return this.reloadFuture = CompletableFuture.allOf(this.recompileFuture, this.updateBuffersFuture).thenCompose(
                unused -> CompletableFuture.supplyAsync(() -> {
                            FileToIdConverter lister = this.sourceSet.getShaderDefinitionLister();
                            Set<ResourceLocation> shaderIds = lister.listMatchingResources(resourceManager).keySet()
                                    .stream()
                                    .map(lister::fileToId)
                                    .collect(Collectors.toSet());
                            return this.prepare(resourceManager, shaderIds);
                        }, backgroundExecutor)
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

    private record ReloadState(Map<ResourceLocation, ProgramDefinition> definitions,
                               Map<ResourceLocation, Resource> shaderSources) {
    }
}
