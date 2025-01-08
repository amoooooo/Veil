package foundry.veil.api.client.render.post.stage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.Veil;
import foundry.veil.api.client.registry.PostPipelineStageRegistry;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.util.Optional;

/**
 * A basic stage that draws a quad to the output using a specified shader.
 *
 * @author Ocelot
 */
public class BlitPostStage extends FramebufferPostStage {

    public static final MapCodec<BlitPostStage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("shader").forGetter(BlitPostStage::getShaderId),
            FramebufferManager.FRAMEBUFFER_CODEC.optionalFieldOf("in").forGetter(stage -> Optional.ofNullable(stage.getIn())),
            FramebufferManager.FRAMEBUFFER_CODEC.optionalFieldOf("out", VeilFramebuffers.POST).forGetter(BlitPostStage::getOut),
            Codec.BOOL.optionalFieldOf("clear", true).forGetter(BlitPostStage::clearOut)
    ).apply(instance, (shader, in, out, clear) -> new BlitPostStage(shader, in.orElse(null), out, clear)));

    private final ResourceLocation shader;
    private boolean printedError;

    /**
     * Creates a new blit post stage that applies the specified shader.
     *
     * @param shader The shader to apply
     * @param in     The framebuffer to use as <code>DiffuseSampler0</code>-<code>DiffuseSampler7</code>
     *               and <code>DiffuseDepthSampler</code>
     * @param out    The framebuffer to write into
     * @param clear  Whether to clear the output before drawing
     */
    public BlitPostStage(ResourceLocation shader, @Nullable ResourceLocation in, ResourceLocation out, boolean clear) {
        super(in, out, clear);
        this.shader = shader;
    }

    @Override
    public void apply(Context context) {
        ShaderProgram shader = VeilRenderSystem.renderer().getShaderManager().getShader(this.shader);
        if (shader == null) {
            if (!this.printedError) {
                this.printedError = true;
                Veil.LOGGER.warn("Failed to find post shader: {}", this.shader);
            }
            return;
        }

        if (this.getOut().equals(this.getIn())) {
            if (!this.printedError) {
                this.printedError = true;
                Veil.LOGGER.error("Input and output targets cannot be the same: {}", this.shader);
            }
        }

        shader.bind();
        context.applySamplers(shader);
        this.setupFramebuffer(context, shader);
        shader.toShaderInstance().setDefaultUniforms(VertexFormat.Mode.TRIANGLE_STRIP, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.applyShaderSamplers(context, 0);
        VeilRenderSystem.drawScreenQuad();
        context.clearSamplers(shader);
    }

    @Override
    public PostPipelineStageRegistry.PipelineType<? extends PostPipeline> getType() {
        return PostPipelineStageRegistry.BLIT.get();
    }

    @Override
    public boolean hasUniform(CharSequence name) {
        ShaderProgram shader = this.getShader();
        return shader != null && shader.hasUniform(name);
    }

    @Override
    public boolean hasUniformBlock(CharSequence name) {
        ShaderProgram shader = this.getShader();
        return shader != null && shader.hasUniformBlock(name);
    }

    @Override
    public boolean hasStorageBlock(CharSequence name) {
        ShaderProgram shader = this.getShader();
        return shader != null && shader.hasStorageBlock(name);
    }

    @Override
    public void setUniformBlock(CharSequence name, int binding) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setUniformBlock(name, binding);
        }
    }

    @Override
    public void setStorageBlock(CharSequence name, int binding) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setStorageBlock(name, binding);
        }
    }

    @Override
    public void setFloat(CharSequence name, float value) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setFloat(name, value);
        }
    }

    @Override
    public void setVector(CharSequence name, float x, float y) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVector(name, x, y);
        }
    }

    @Override
    public void setVector(CharSequence name, float x, float y, float z) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVector(name, x, y, z);
        }
    }

    @Override
    public void setVector(CharSequence name, float x, float y, float z, float w) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVector(name, x, y, z, w);
        }
    }

    @Override
    public void setInt(CharSequence name, int value) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setInt(name, value);
        }
    }

    @Override
    public void setVectorI(CharSequence name, int x, int y) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVectorI(name, x, y);
        }
    }

    @Override
    public void setVectorI(CharSequence name, int x, int y, int z) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVectorI(name, x, y, z);
        }
    }

    @Override
    public void setVectorI(CharSequence name, int x, int y, int z, int w) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVectorI(name, x, y, z, w);
        }
    }

    @Override
    public void setFloats(CharSequence name, float... values) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setFloats(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector2fc... values) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVectors(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector3fc... values) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVectors(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector4fc... values) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVectors(name, values);
        }
    }

    @Override
    public void setInts(CharSequence name, int... values) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setInts(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector2ic... values) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVectors(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector3ic... values) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVectors(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector4ic... values) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setVectors(name, values);
        }
    }

    @Override
    public void setMatrix(CharSequence name, Matrix2fc value, boolean transpose) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setMatrix(name, value, transpose);
        }
    }

    @Override
    public void setMatrix(CharSequence name, Matrix3fc value, boolean transpose) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setMatrix(name, value, transpose);
        }
    }

    @Override
    public void setMatrix(CharSequence name, Matrix3x2fc value, boolean transpose) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setMatrix(name, value, transpose);
        }
    }

    @Override
    public void setMatrix(CharSequence name, Matrix4fc value, boolean transpose) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setMatrix(name, value, transpose);
        }
    }

    @Override
    public void setMatrix(CharSequence name, Matrix4x3fc value, boolean transpose) {
        ShaderProgram shader = this.getShader();
        if (shader != null) {
            shader.setMatrix(name, value, transpose);
        }
    }

    /**
     * @return The shader this stage should use
     */
    public @Nullable ShaderProgram getShader() {
        return VeilRenderSystem.renderer().getShaderManager().getShader(this.shader);
    }

    /**
     * @return The name of the shader this stage should use
     */
    public ResourceLocation getShaderId() {
        return this.shader;
    }
}
