package foundry.veil.api.client.render.post.stage;

import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract stage that uses a framebuffer as the input and output.
 *
 * @author Ocelot
 */
public abstract class FramebufferPostStage implements PostPipeline {

    private final ResourceLocation in;
    private final ResourceLocation out;
    private final boolean clear;

    /**
     * Creates a post stage with the specified input and output framebuffers.
     *
     * @param in    The framebuffer to use as <code>DiffuseSampler0</code>-<code>DiffuseSampler...max</code>
     *              and <code>DiffuseDepthSampler</code>
     * @param out   The framebuffer to write into
     * @param clear Whether to clear the output before drawing
     */
    public FramebufferPostStage(@Nullable ResourceLocation in, ResourceLocation out, boolean clear) {
        this.in = in;
        this.out = out;
        this.clear = clear;
    }

    /**
     * Applies the input framebuffer textures and binds the output framebuffer.
     *
     * @param context The context for post-processing
     * @param shader  The shader to set input samplers to
     */
    protected void setupFramebuffer(Context context, ShaderProgram shader) {
        AdvancedFbo in = this.in != null ? context.getFramebuffer(this.in) : null;
        AdvancedFbo out = context.getFramebufferOrDraw(this.out);

        if (in != null) {
            shader.setFramebufferSamplers(in);
        }

        out.bind(true);
        if (this.clear) {
            out.clear();
        }

        if (in != null) {
            shader.setVector("InSize", in.getWidth(), in.getHeight());
        } else {
            shader.setVector("InSize", 1.0F, 1.0F);
        }

        shader.setVector("OutSize", out.getWidth(), out.getHeight());
    }

    /**
     * @return The framebuffer to read from
     */
    public @Nullable ResourceLocation getIn() {
        return this.in;
    }

    /**
     * @return The framebuffer to write into
     */
    public ResourceLocation getOut() {
        return this.out;
    }

    /**
     * @return Whether the output should be cleared before drawing
     */
    public boolean clearOut() {
        return this.clear;
    }
}
