package foundry.veil.api.client.render.post.stage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.registry.PostPipelineStageRegistry;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.post.PostPipeline;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL20.GL_MAX_DRAW_BUFFERS;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_MAX_COLOR_ATTACHMENTS;
import static org.lwjgl.opengl.GL30.glBlitFramebuffer;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;

/**
 * Copies data from one framebuffer to another.
 *
 * @author Ocelot
 */
public class CopyPostStage extends FramebufferPostStage {

    public static final Codec<CopyPostStage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FramebufferManager.FRAMEBUFFER_CODEC.fieldOf("in").forGetter(CopyPostStage::getIn),
            Codec.INT.optionalFieldOf("in_attachment", 0)
                .flatXmap(
                    a -> a >= 0 && a < glGetInteger(GL_MAX_COLOR_ATTACHMENTS)
                        ? DataResult.success(a)
                        : DataResult.error(() -> "Invalid in attachment: " + a + ", must be in range of [0, " + glGetInteger(GL_MAX_COLOR_ATTACHMENTS) + ")"),
                    DataResult::success
                ).forGetter(CopyPostStage::getInAttachment),
            FramebufferManager.FRAMEBUFFER_CODEC.fieldOf("out").forGetter(CopyPostStage::getOut),
            Codec.list(Codec.INT).optionalFieldOf("out_attachments", List.of(0))
                .flatXmap(as -> {
                        for (int a : as) {
                            if (a < 0 || a >= glGetInteger(GL_MAX_DRAW_BUFFERS))
                                return DataResult.error(() -> "Invalid out attachment: " + a + ", must be in range of [0, " + glGetInteger(GL_MAX_DRAW_BUFFERS) + ")");
                        }
                        if (as.stream().distinct().count() != as.size())
                            return DataResult.error(() -> "Out attachments contains duplicates");
                        return DataResult.success(as);
                    },
                    DataResult::success
                ).forGetter(CopyPostStage::getOutAttachments),
            Codec.BOOL.optionalFieldOf("color", true).forGetter(CopyPostStage::copyColor),
            Codec.BOOL.optionalFieldOf("depth", false).forGetter(CopyPostStage::copyDepth),
            Codec.BOOL.optionalFieldOf("linear", false).forGetter(CopyPostStage::isLinear)
    ).apply(instance, CopyPostStage::new));

    private final int inAttachment;
    private final int[] outAttachments;
    private final int mask;
    private final int filter;

    /**
     * Creates a new blit post stage that applies the specified shader.
     *
     * @param in             The framebuffer to copy from
     * @param inAttachment   The color attachment to copy from, must be in the range of [0, GL_MAX_COLOR_ATTACHMENTS), see also: {@link org.lwjgl.opengl.GL11#glReadBuffer(int)}
     * @param out            The framebuffer to write into
     * @param outAttachments The color attachments to write into, the values must be in the range of [0, GL_MAX_DRAW_BUFFERS), see also: {@link org.lwjgl.opengl.GL30#glDrawBuffers(int[])}
     * @param copyColor      Whether to copy the color buffers
     * @param copyDepth      Whether to copy the depth buffers
     * @param linear         Whether to copy with a linear filter if the input size doesn't match the output size
     */
    public CopyPostStage(ResourceLocation in, int inAttachment, ResourceLocation out, List<Integer> outAttachments, boolean copyColor, boolean copyDepth, boolean linear) {
        super(in, out, false);
        this.inAttachment = inAttachment + GL_COLOR_ATTACHMENT0;
        this.outAttachments = outAttachments.stream().mapToInt(a -> a + GL_COLOR_ATTACHMENT0).toArray();
        this.mask = (copyColor ? GL_COLOR_BUFFER_BIT : 0) | (copyDepth ? GL_DEPTH_BUFFER_BIT : 0);
        this.filter = linear ? GL_LINEAR : GL_NEAREST;
    }

    @Override
    public void apply(PostPipeline.Context context) {
        AdvancedFbo in = context.getFramebuffer(this.getIn());
        AdvancedFbo out = context.getFramebuffer(this.getOut());
        if (in != null && out != null) {

            in.bindRead();
            out.bindDraw(false);

            int old_read_buf = glGetInteger(GL_READ_BUFFER);
            if (this.inAttachment != old_read_buf) glReadBuffer(this.inAttachment);
            boolean draw_bufs_changed = false;
            if (!Arrays.equals(this.outAttachments, out.getDrawBuffers())) {
                draw_bufs_changed = true;
                glDrawBuffers(this.outAttachments);
            }

            glBlitFramebuffer(0, 0, in.getWidth(), in.getHeight(), 0, 0, out.getWidth(), out.getHeight(), mask, filter);

            if (this.inAttachment != old_read_buf) glReadBuffer(old_read_buf);
            if (draw_bufs_changed) glDrawBuffers(out.getDrawBuffers());

            AdvancedFbo.unbind();
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
     * @return The color attachment to copy from, must be in the range of [0, GL_MAX_COLOR_ATTACHMENTS), see also: {@link org.lwjgl.opengl.GL11#glReadBuffer(int)}
     */
    public int getInAttachment() {
        return this.inAttachment - GL_COLOR_ATTACHMENT0;
    }

    /**
     * @return The color attachments to write into, the values must be in the range of [0, GL_MAX_DRAW_BUFFERS), see also: {@link org.lwjgl.opengl.GL30#glDrawBuffers(int[])}
     */
    public List<Integer> getOutAttachments() {
        return Arrays.stream(this.outAttachments).mapToObj(a -> a - GL_COLOR_ATTACHMENT0).toList();
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
