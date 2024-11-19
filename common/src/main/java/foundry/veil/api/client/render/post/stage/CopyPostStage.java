package foundry.veil.api.client.render.post.stage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.registry.PostPipelineStageRegistry;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.post.PostPipeline;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;

/**
 * Copies data from one framebuffer to another.
 *
 * @author Ocelot
 */
public class CopyPostStage extends FramebufferPostStage {

    public static final MapCodec<CopyPostStage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FramebufferManager.FRAMEBUFFER_CODEC.fieldOf("in").forGetter(CopyPostStage::getIn),
            FramebufferManager.FRAMEBUFFER_CODEC.fieldOf("out").forGetter(CopyPostStage::getOut),
            Codec.BOOL.optionalFieldOf("color", true).forGetter(CopyPostStage::copyColor),
            Codec.BOOL.optionalFieldOf("depth", false).forGetter(CopyPostStage::copyDepth),
            Codec.BOOL.optionalFieldOf("linear", false).forGetter(CopyPostStage::isLinear)
    ).apply(instance, CopyPostStage::new));

    private final int mask;
    private final int filter;

    /**
     * Creates a new blit post stage that applies the specified shader.
     *
     * @param in        The framebuffer to copy from
     * @param out       The framebuffer to write into
     * @param copyColor Whether to copy the color buffers
     * @param copyDepth Whether to copy the depth buffers
     * @param linear    Whether to copy with a linear filter if the input size doesn't match the output size
     */
    public CopyPostStage(ResourceLocation in, ResourceLocation out, boolean copyColor, boolean copyDepth, boolean linear) {
        super(in, out, false);
        this.mask = (copyColor ? GL_COLOR_BUFFER_BIT : 0) | (copyDepth ? GL_DEPTH_BUFFER_BIT : 0);
        this.filter = linear ? GL_LINEAR : GL_NEAREST;
    }

    @Override
    public void apply(PostPipeline.Context context) {
        AdvancedFbo in = context.getFramebuffer(this.getIn());
        AdvancedFbo out = context.getFramebuffer(this.getOut());
        if (in != null && out != null) {
            in.resolveToAdvancedFbo(out, this.mask, this.filter);
        }
    }

    @Override
    public PostPipelineStageRegistry.PipelineType<? extends PostPipeline> getType() {
        return PostPipelineStageRegistry.COPY.get();
    }

    @Override
    public ResourceLocation getIn() {
        return Objects.requireNonNull(super.getIn());
    }

    /**
     * @return The mask to use when copying from one buffer to another
     */
    public int getMask() {
        return this.mask;
    }

    /**
     * @return The filter to use when copying from one buffer to another
     */
    public int getFilter() {
        return this.filter;
    }

    /**
     * @return Whether color is copied from the buffer
     */
    public boolean copyColor() {
        return (this.mask & GL_COLOR_BUFFER_BIT) > 0;
    }

    /**
     * @return Whether depth is copied from the buffer
     */
    public boolean copyDepth() {
        return (this.mask & GL_DEPTH_BUFFER_BIT) > 0;
    }

    /**
     * @return Whether to copy with a linear filter if the input size doesn't match the output size
     */
    public boolean isLinear() {
        return this.filter == GL_LINEAR;
    }
}
