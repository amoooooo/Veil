package foundry.veil.forge.event;

import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

/**
 * <p>Events fired when Veil runs post-processing.</p>
 *
 * <p><b><i>Note: These events are only fired if there are post-processing steps to run.</i></b></p>
 *
 * @author Ocelot
 * @see PostProcessingManager
 */
public class ForgeVeilPostProcessingEvent extends Event {

    private final ResourceLocation name;
    private final PostPipeline pipeline;
    private final PostPipeline.Context context;

    public ForgeVeilPostProcessingEvent(ResourceLocation name, PostPipeline pipeline, PostPipeline.Context context) {
        this.name = name;
        this.pipeline = pipeline;
        this.context = context;
    }

    /**
     * @return The name of the pipeline running
     */
    public ResourceLocation getName() {
        return this.name;
    }

    /**
     * @return The pipeline instance
     */
    public PostPipeline getPipeline() {
        return this.pipeline;
    }

    /**
     * @return The context for running pipelines
     */
    public PostPipeline.Context getContext() {
        return this.context;
    }

    /**
     * Fired before Veil runs the default post-processing steps.
     *
     * @author Ocelot
     */
    public static class Pre extends ForgeVeilPostProcessingEvent {

        public Pre(ResourceLocation name, PostPipeline pipeline, PostPipeline.Context context) {
            super(name, pipeline, context);
        }
    }

    /**
     * Fired after Veil runs the default post-processing steps.
     *
     * @author Ocelot
     */
    public static class Post extends ForgeVeilPostProcessingEvent {

        public Post(ResourceLocation name, PostPipeline pipeline, PostPipeline.Context context) {
            super(name, pipeline, context);
        }
    }
}
