package foundry.veil.api.client.render.shader.program;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.util.EnumCodec;

import static org.lwjgl.opengl.GL14C.*;
import static org.lwjgl.opengl.GL20C.glBlendEquationSeparate;

/**
 * Specifies the blend mode for a {@link ShaderProgram}.
 *
 * @param colorEquation  The color component equation. The default is {@link BlendEquation#ADD}
 * @param alphaEquation  The alpha component equation. The default is {@link BlendEquation#ADD}
 * @param srcColorFactor The source color factor. The default is {@link GlStateManager.SourceFactor#ONE}
 * @param dstColorFactor The destination color factor. The default is {@link GlStateManager.DestFactor#ONE}
 * @param srcAlphaFactor The source alpha factor. The default is {@link GlStateManager.SourceFactor#ONE}
 * @param dstAlphaFactor The destination alpha factor. The default is {@link GlStateManager.DestFactor#ONE}
 * @author Ocelot
 */
public record ShaderBlendMode(
        BlendEquation colorEquation,
        BlendEquation alphaEquation,
        GlStateManager.SourceFactor srcColorFactor,
        GlStateManager.DestFactor dstColorFactor,
        GlStateManager.SourceFactor srcAlphaFactor,
        GlStateManager.DestFactor dstAlphaFactor
) {

    public static final Codec<GlStateManager.SourceFactor> SOURCE_FACTOR_CODEC = EnumCodec
            .<GlStateManager.SourceFactor>builder("Source Factor")
            .values(GlStateManager.SourceFactor.class)
            .build();
    public static final Codec<GlStateManager.DestFactor> DESTINATION_FACTOR_CODEC = EnumCodec
            .<GlStateManager.DestFactor>builder("Destination Factor")
            .values(GlStateManager.DestFactor.class)
            .build();

    public static final Codec<ShaderBlendMode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlendEquation.CODEC.optionalFieldOf("func", BlendEquation.ADD).forGetter(ShaderBlendMode::colorEquation),
            BlendEquation.CODEC.optionalFieldOf("alphafunc", BlendEquation.ADD).forGetter(ShaderBlendMode::alphaEquation),
            SOURCE_FACTOR_CODEC.optionalFieldOf("srcrgb", GlStateManager.SourceFactor.ONE).forGetter(ShaderBlendMode::srcColorFactor),
            DESTINATION_FACTOR_CODEC.optionalFieldOf("dstrgb", GlStateManager.DestFactor.ZERO).forGetter(ShaderBlendMode::dstColorFactor),
            SOURCE_FACTOR_CODEC.optionalFieldOf("srcalpha", GlStateManager.SourceFactor.ONE).forGetter(ShaderBlendMode::srcAlphaFactor),
            DESTINATION_FACTOR_CODEC.optionalFieldOf("dstalpha", GlStateManager.DestFactor.ZERO).forGetter(ShaderBlendMode::dstAlphaFactor)
    ).apply(instance, ShaderBlendMode::new));

    /**
     * Applies this blend mode.
     */
    public void apply() {
        if (this.colorEquation != BlendEquation.ADD || this.alphaEquation != BlendEquation.ADD) {
            glBlendEquationSeparate(this.colorEquation.getGlType(), this.alphaEquation.getGlType());
        }
        RenderSystem.blendFuncSeparate(this.srcColorFactor, this.dstColorFactor, this.srcAlphaFactor, this.dstAlphaFactor);
    }

    /**
     * @return Whether the blend equations have been changed from {@link BlendEquation#ADD}
     */
    public boolean hasEquation() {
        return this.colorEquation != BlendEquation.ADD || this.alphaEquation != BlendEquation.ADD;
    }

    /**
     * Possible OpenGL blend equations.
     *
     * @author Ocelot
     */
    public enum BlendEquation {
        ADD(GL_FUNC_ADD),
        SUBTRACT(GL_FUNC_SUBTRACT),
        REVERSE_SUBTRACT(GL_FUNC_REVERSE_SUBTRACT),
        MIN(GL_MIN),
        MAX(GL_MAX);

        public static final Codec<BlendEquation> CODEC = EnumCodec
                .<BlendEquation>builder("Blend Equation")
                .values(BlendEquation.class)
                .build();

        private final int glType;

        BlendEquation(int glType) {
            this.glType = glType;
        }

        /**
         * @return The OpenGL enum
         */
        public int getGlType() {
            return this.glType;
        }
    }
}
