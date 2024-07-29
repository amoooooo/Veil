package foundry.veil.api.client.render.post.stage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.registry.PostPipelineStageRegistry;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.post.PostPipeline;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
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
            Codec.STRING.optionalFieldOf("in_attachment").forGetter(CopyPostStage::getInAttachmentName),
            FramebufferManager.FRAMEBUFFER_CODEC.fieldOf("out").forGetter(CopyPostStage::getOut),
            Codec.list(Codec.STRING).optionalFieldOf("out_attachments").forGetter(CopyPostStage::getOutAttachmentNames),
            Codec.BOOL.optionalFieldOf("color", true).forGetter(CopyPostStage::copyColor),
            Codec.BOOL.optionalFieldOf("depth", false).forGetter(CopyPostStage::copyDepth),
            Codec.BOOL.optionalFieldOf("linear", false).forGetter(CopyPostStage::isLinear)
    ).apply(instance, CopyPostStage::new));

    private final @Nullable String inAttachmentName;
    private int inAttachmentId = 0;
    private final @Nullable List<String> outAttachmentNames;
    private int[] outAttachmentIds = null;
    private final int mask;
    private final int filter;

    /**
     * Creates a new blit post stage that applies the specified shader.
     *
     * @param in                 The framebuffer to copy from
     * @param inAttachmentName   The name of the color attachment to copy from, see also: {@link org.lwjgl.opengl.GL11#glReadBuffer(int)}
     * @param out                The framebuffer to write into
     * @param outAttachmentNames The names of the color attachments to write into, defaults to color attachment 0, see also: {@link org.lwjgl.opengl.GL30#glDrawBuffers(int[])}
     * @param copyColor          Whether to copy the color buffers
     * @param copyDepth          Whether to copy the depth buffers
     * @param linear             Whether to copy with a linear filter if the input size doesn't match the output size
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public CopyPostStage(ResourceLocation in, Optional<String> inAttachmentName, ResourceLocation out, Optional<List<String>> outAttachmentNames, boolean copyColor, boolean copyDepth, boolean linear) {
        super(in, out, List.of());
        this.inAttachmentName = inAttachmentName.orElse(null);
        this.outAttachmentNames = outAttachmentNames.orElse(null);
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

            // Update attachment ids
            if (this.inAttachmentId == 0) {
                if (this.inAttachmentName == null) {
                    this.inAttachmentId = GL_COLOR_ATTACHMENT0;
                } else {
                    this.inAttachmentId = in.getColorAttachmentSlot(this.inAttachmentName)
                            .map(i -> i + GL_COLOR_ATTACHMENT0)
                            .orElse(GL_COLOR_ATTACHMENT0);
                }
            }
            if (this.outAttachmentIds == null) {
                if (this.outAttachmentNames == null) {
                    this.outAttachmentIds = new int[]{GL_COLOR_ATTACHMENT0};
                } else {
                    this.outAttachmentIds = this.outAttachmentNames.stream().filter(
                            name -> {
                                if (out.getColorAttachmentSlot(name).isEmpty()) {
                                    throw new InvalidParameterException("Invalid out attachment name \"" + name + "\" in CopyPostStage.");
                                }
                                return true;
                            }
                    ).mapToInt(name -> out.getColorAttachmentSlot(name).orElseThrow() + GL_COLOR_ATTACHMENT0).toArray();
                }
            }

            int old_read_buf = glGetInteger(GL_READ_BUFFER);
            if (this.inAttachmentId != old_read_buf) glReadBuffer(this.inAttachmentId);
            boolean draw_bufs_changed = false;
            if (!Arrays.equals(this.outAttachmentIds, out.getDrawBuffers())) {
                draw_bufs_changed = true;
                glDrawBuffers(this.outAttachmentIds);
            }

            glBlitFramebuffer(0, 0, in.getWidth(), in.getHeight(), 0, 0, out.getWidth(), out.getHeight(), mask, filter);

            if (this.inAttachmentId != old_read_buf) glReadBuffer(old_read_buf);
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
     * @return The name of the color attachment to copy from, see also: {@link org.lwjgl.opengl.GL11#glReadBuffer(int)}
     */
    private Optional<String> getInAttachmentName() {
        return Optional.ofNullable(this.inAttachmentName);
    }

    /**
     * @return The names of the color attachments to write into, see also: {@link org.lwjgl.opengl.GL30#glDrawBuffers(int[])}
     */
    private Optional<List<String>> getOutAttachmentNames() {
        return Optional.ofNullable(this.outAttachmentNames);
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
