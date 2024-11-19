package foundry.veil.api.client.render.post.stage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.registry.PostPipelineStageRegistry;
import foundry.veil.api.client.render.post.PostPipeline;

/**
 * Sets the color and depth masks.
 *
 * @param red   Whether red values will be written to the screen
 * @param green Whether green values will be written to the screen
 * @param blue  Whether blue values will be written to the screen
 * @param alpha Whether alpha values will be written to the screen
 * @param depth Whether depth values will be written to the screen
 * @author Ocelot
 */
public record MaskPostStage(boolean red,
                            boolean green,
                            boolean blue,
                            boolean alpha,
                            boolean depth) implements PostPipeline {

    public static final MapCodec<MaskPostStage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("red", true).forGetter(MaskPostStage::red),
            Codec.BOOL.optionalFieldOf("green", true).forGetter(MaskPostStage::green),
            Codec.BOOL.optionalFieldOf("blue", true).forGetter(MaskPostStage::blue),
            Codec.BOOL.optionalFieldOf("alpha", true).forGetter(MaskPostStage::alpha),
            Codec.BOOL.optionalFieldOf("depth", false).forGetter(MaskPostStage::depth)
    ).apply(instance, MaskPostStage::new));

    @Override
    public void apply(PostPipeline.Context context) {
        RenderSystem.colorMask(this.red, this.green, this.blue, this.alpha);
        RenderSystem.depthMask(this.depth);
    }

    @Override
    public PostPipelineStageRegistry.PipelineType<? extends PostPipeline> getType() {
        return PostPipelineStageRegistry.MASK.get();
    }
}
