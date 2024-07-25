package foundry.veil.api.client.render.post;

import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import foundry.veil.Veil;
import foundry.veil.VeilClient;
import foundry.veil.api.CodecReloadListener;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.post.stage.CompositePostPipeline;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.pipeline.PostPipelineContext;
import foundry.veil.platform.VeilClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static org.lwjgl.opengl.GL11C.GL_ALWAYS;
import static org.lwjgl.opengl.GL11C.GL_LEQUAL;

/**
 * <p>Manages all post pipelines.</p>
 * <p>Post Pipelines are a single "effect" that can be applied.
 * For example, a vanilla Minecraft creeper effect can be a added using {@link #add(int, ResourceLocation)}</p>
 *
 * @author Ocelot
 */
public class PostProcessingManager extends CodecReloadListener<CompositePostPipeline> implements NativeResource {

    private static final Comparator<ProfileEntry> PIPELINE_SORTER = Comparator.comparingInt(ProfileEntry::getPriority).reversed();

    private final PostPipelineContext context;
    private final List<ProfileEntry> activePipelines;
    private final List<ProfileEntry> activePipelinesView;
    private final Map<ResourceLocation, PostPipeline> pipelines;

    /**
     * Creates a new instance of the post-processing manager.
     */
    public PostProcessingManager() {
        super(CompositePostPipeline.CODEC, FileToIdConverter.json("pinwheel/post"));
        this.context = new PostPipelineContext();
        this.activePipelines = new LinkedList<>();
        this.activePipelinesView = Collections.unmodifiableList(this.activePipelines);
        this.pipelines = new HashMap<>();
    }

    /**
     * Checks to see if the specified pipeline is active.
     *
     * @param pipeline The pipeline to check for
     * @return Whether that pipeline is active
     */
    public boolean isActive(ResourceLocation pipeline) {
        return this.activePipelines.stream().anyMatch(entry -> entry.pipeline.equals(pipeline));
    }

    /**
     * Adds the specified pipeline under the default priority of <code>1000</code>.
     * A higher priority indicates the pipeline should be run earlier than lower priority pipelines.
     *
     * @param pipeline The pipeline to add
     * @return Whether the pipeline was added or had a priority change
     */
    public boolean add(ResourceLocation pipeline) {
        return this.add(1000, pipeline);
    }

    /**
     * Adds the specified pipeline with the specified priority.
     * A higher priority indicates the pipeline should be run earlier than lower priority pipelines.
     *
     * @param priority The priority to set the pipeline to. The default priority is <code>1000</code>
     * @param pipeline The pipeline to add
     * @return Whether the pipeline was added or had a priority change
     */
    public boolean add(int priority, ResourceLocation pipeline) {
        if (this.activePipelines.stream().anyMatch(entry -> entry.priority == priority && entry.pipeline.equals(pipeline))) {
            return false;
        }
        this.activePipelines.removeIf(entry -> entry.pipeline.equals(pipeline));
        this.activePipelines.add(new ProfileEntry(pipeline, priority));
        return true;
    }

    /**
     * Removes the specified pipeline from the active profiles.
     *
     * @param pipeline The pipeline to remove
     * @return If the pipeline was previously active
     */
    public boolean remove(ResourceLocation pipeline) {
        return this.activePipelines.removeIf(entry -> entry.pipeline.equals(pipeline));
    }

    /**
     * Retrieves a post pipeline by name.
     *
     * @param pipeline The pipeline to get
     * @return The pipeline found or <code>null</code> if it doesn't exist
     */
    public @Nullable PostPipeline getPipeline(ResourceLocation pipeline) {
        return this.pipelines.get(pipeline);
    }

    private void setup() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL_ALWAYS);
        RenderSystem.depthMask(false);
    }

    private void clear() {
        ShaderProgram.unbind();
        AdvancedFbo.unbind();
        RenderSystem.depthFunc(GL_LEQUAL);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
    }

    private void clearPipeline() {
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthFunc(GL_ALWAYS);
        RenderSystem.depthMask(false);
    }

    @ApiStatus.Internal
    public void runPipeline() {
        if (!this.activePipelines.isEmpty()) {
            VeilClientPlatform platform = VeilClient.clientPlatform();
            this.context.begin();
            this.setup();
            int activeTexture = GlStateManager._getActiveTexture();

            this.activePipelines.sort(PIPELINE_SORTER);
            for (ProfileEntry entry : this.activePipelines) {
                ResourceLocation id = entry.getPipeline();
                PostPipeline pipeline = this.pipelines.get(id);
                if (pipeline != null) {
                    platform.preVeilPostProcessing(id, pipeline, this.context);
                    try {
                        pipeline.apply(this.context);
                        this.clearPipeline();
                    } catch (Exception e) {
                        Veil.LOGGER.error("Error running pipeline {}", id, e);
                    }
                    platform.postVeilPostProcessing(id, pipeline, this.context);
                }
            }

            RenderSystem.activeTexture(activeTexture);
            this.clear();
            this.context.end();
        }

        AdvancedFbo postFramebuffer = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(VeilFramebuffers.POST);
        if (postFramebuffer != null) {
            postFramebuffer.resolveToFramebuffer(Minecraft.getInstance().getMainRenderTarget());
        }
    }

    /**
     * Applies only the specified pipeline. Copies the main buffer into the post framebuffer and back before running the pipeline.
     *
     * @param pipeline The pipeline to run
     */
    public void runPipeline(PostPipeline pipeline) {
        this.runPipeline(pipeline, true);
    }

    /**
     * Applies only the specified pipeline.
     *
     * @param pipeline    The pipeline to run
     * @param resolvePost Whether to copy the main buffer into the post framebuffer before running the pipeline
     */
    public void runPipeline(PostPipeline pipeline, boolean resolvePost) {
        AdvancedFbo postFramebuffer = resolvePost ? VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(VeilFramebuffers.POST) : null;
        if (postFramebuffer != null) {
            AdvancedFbo.getMainFramebuffer().resolveToAdvancedFbo(postFramebuffer);
        }

        this.context.begin();
        this.setup();
        int activeTexture = GlStateManager._getActiveTexture();

        try {
            pipeline.apply(this.context);
            this.clearPipeline();
        } catch (Exception e) {
            Veil.LOGGER.error("Error running pipeline {}", pipeline, e);
        }

        RenderSystem.activeTexture(activeTexture);
        this.clear();
        this.context.end();

        if (postFramebuffer != null) {
            postFramebuffer.resolveToFramebuffer(Minecraft.getInstance().getMainRenderTarget());
        }
    }

    public void runPipeline(ResourceLocation id, boolean resolvePost) {
        runPipeline(getPipeline(id), resolvePost);
    }

    public void runPipeline(ResourceLocation id) {
        runPipeline(getPipeline(id));
    }

    private CompositePostPipeline loadPipeline(Resource resource) throws IOException {
        try (Reader reader = resource.openAsReader()) {
            JsonElement element = JsonParser.parseReader(reader);
            DataResult<CompositePostPipeline> result = this.codec.parse(JsonOps.INSTANCE, element);

            if (result.error().isPresent()) {
                throw new JsonSyntaxException(result.error().get().message());
            }
            return result.result().orElseThrow();
        }
    }

    @Override
    protected @NotNull Map<ResourceLocation, CompositePostPipeline> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, CompositePostPipeline> data = new HashMap<>();

        Map<ResourceLocation, List<Resource>> resources = this.converter.listMatchingResourceStacks(resourceManager);
        for (Map.Entry<ResourceLocation, List<Resource>> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            ResourceLocation id = this.converter.fileToId(location);

            if (entry.getValue().size() == 1) {
                try {
                    data.put(id, this.loadPipeline(Iterables.getOnlyElement(entry.getValue())));
                } catch (Exception e) {
                    Veil.LOGGER.error("Couldn't parse data file {} from {}", id, location, e);
                }
                continue;
            }

            List<CompositePostPipeline> pipelines = new ArrayList<>(entry.getValue().size());
            for (Resource resource : entry.getValue()) {
                try {
                    pipelines.add(this.loadPipeline(resource));
                } catch (Exception e) {
                    Veil.LOGGER.error("Couldn't parse data file {} from {}", id, location, e);
                }
            }

            // No pipelines loaded, so continue
            if (pipelines.isEmpty()) {
                continue;
            }

            // Only 1 pipeline successfully loaded
            if (pipelines.size() == 1) {
                data.put(id, Iterables.getOnlyElement(pipelines));
                continue;
            }

            pipelines.sort(Comparator.comparingInt(CompositePostPipeline::getPriority));
            for (int i = 0; i < pipelines.size(); i++) {
                CompositePostPipeline pipeline = pipelines.get(i);
                if (pipeline.isReplace()) {
                    pipelines = pipelines.subList(0, i + 1);
                    break;
                }
            }
            data.put(id, new CompositePostPipeline(pipelines.toArray(CompositePostPipeline[]::new), Collections.emptyMap(), Collections.emptyMap()));
        }

        return data;
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, CompositePostPipeline> data, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        this.pipelines.values().forEach(PostPipeline::free);
        this.pipelines.clear();
        this.pipelines.putAll(data);
        Veil.LOGGER.info("Loaded {} post pipelines", this.pipelines.size());
    }

    @Override
    public void free() {
        this.pipelines.values().forEach(PostPipeline::free);
        this.pipelines.clear();
        this.context.free();
    }

    /**
     * @return The default context for post-processing
     */
    public PostPipeline.Context getContext() {
        return this.context;
    }

    /**
     * @return All available pipelines
     */
    public @NotNull Set<ResourceLocation> getPipelines() {
        return this.pipelines.keySet();
    }

    /**
     * @return An immutable view of all active profiles and their priorities
     */
    public List<ProfileEntry> getActivePipelines() {
        return this.activePipelinesView;
    }

    /**
     * A single active profile.
     */
    public static class ProfileEntry {

        private final ResourceLocation pipeline;
        private int priority;

        public ProfileEntry(ResourceLocation pipeline, int priority) {
            this.pipeline = pipeline;
            this.priority = priority;
        }

        /**
         * @return The id of the pipeline shader
         */
        public ResourceLocation getPipeline() {
            return this.pipeline;
        }

        /**
         * @return The priority the profile is inserted at
         */
        public int getPriority() {
            return this.priority;
        }

        /**
         * Sets the priority this effect is applied at.
         *
         * @param priority The new priority
         */
        public void setPriority(int priority) {
            this.priority = priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            ProfileEntry that = (ProfileEntry) o;
            return this.priority == that.priority && Objects.equals(this.pipeline, that.pipeline);
        }

        @Override
        public int hashCode() {
            int result = this.pipeline.hashCode();
            result = 31 * result + this.priority;
            return result;
        }
    }
}
