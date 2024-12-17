package foundry.veil.fabric.mixin.compat.iris;

import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.ext.iris.IrisRenderTargetExtension;
import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.gl.texture.PixelType;
import net.irisshaders.iris.targets.RenderTarget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderTarget.class)
public abstract class IrisRenderTargetMixin implements IrisRenderTargetExtension {

    @Shadow(remap = false)
    private String name;

    @Shadow(remap = false)
    public abstract int getMainTexture();

    @Shadow(remap = false)
    public abstract int getAltTexture();

    @Shadow(remap = false)
    public abstract int getWidth();

    @Shadow(remap = false)
    public abstract int getHeight();

    @Shadow
    public abstract InternalTextureFormat getInternalFormat();

    @Shadow
    @Final
    private PixelType type;

    @Override
    public String veil$getName() {
        return this.name;
    }

    @Override
    public int veil$getMainTexture() {
        return this.getMainTexture();
    }

    @Override
    public int veil$getAltTexture() {
        return this.getAltTexture();
    }

    @Override
    public int veil$getWidth() {
        return this.getWidth();
    }

    @Override
    public int veil$getHeight() {
        return this.getHeight();
    }

    @Override
    public FramebufferAttachmentDefinition.Format veil$getFormat() {
        return switch (this.getInternalFormat()) {
            case RGBA -> FramebufferAttachmentDefinition.Format.RGBA;
            case R8 -> FramebufferAttachmentDefinition.Format.R8;
            case RG8 -> FramebufferAttachmentDefinition.Format.RG8;
            case RGB8 -> FramebufferAttachmentDefinition.Format.RGB8;
            case RGBA8 -> FramebufferAttachmentDefinition.Format.RGBA8;
            case R8_SNORM -> FramebufferAttachmentDefinition.Format.R8_SNORM;
            case RG8_SNORM -> FramebufferAttachmentDefinition.Format.RG8_SNORM;
            case RGB8_SNORM -> FramebufferAttachmentDefinition.Format.RGB8_SNORM;
            case RGBA8_SNORM -> FramebufferAttachmentDefinition.Format.RGBA8_SNORM;
            case R16 -> FramebufferAttachmentDefinition.Format.R16;
            case RG16 -> FramebufferAttachmentDefinition.Format.RG16;
            case RGB16 -> FramebufferAttachmentDefinition.Format.RGB16;
            case RGBA16 -> FramebufferAttachmentDefinition.Format.RGBA16;
            case R16_SNORM -> FramebufferAttachmentDefinition.Format.R16_SNORM;
            case RG16_SNORM -> FramebufferAttachmentDefinition.Format.RG16_SNORM;
            case RGB16_SNORM -> FramebufferAttachmentDefinition.Format.RGB16_SNORM;
            case RGBA16_SNORM -> FramebufferAttachmentDefinition.Format.RGBA16_SNORM;
            case R16F -> FramebufferAttachmentDefinition.Format.R16F;
            case RG16F -> FramebufferAttachmentDefinition.Format.RG16F;
            case RGB16F -> FramebufferAttachmentDefinition.Format.RGB16F;
            case RGBA16F -> FramebufferAttachmentDefinition.Format.RGBA16F;
            case R32F -> FramebufferAttachmentDefinition.Format.R32F;
            case RG32F -> FramebufferAttachmentDefinition.Format.RG32F;
            case RGB32F -> FramebufferAttachmentDefinition.Format.RGB32F;
            case RGBA32F -> FramebufferAttachmentDefinition.Format.RGBA32F;
            case R8I -> FramebufferAttachmentDefinition.Format.R8I;
            case RG8I -> FramebufferAttachmentDefinition.Format.RG8I;
            case RGB8I -> FramebufferAttachmentDefinition.Format.RGB8I;
            case RGBA8I -> FramebufferAttachmentDefinition.Format.RGBA8I;
            case R8UI -> FramebufferAttachmentDefinition.Format.R8UI;
            case RG8UI -> FramebufferAttachmentDefinition.Format.RG8UI;
            case RGB8UI -> FramebufferAttachmentDefinition.Format.RGB8UI;
            case RGBA8UI -> FramebufferAttachmentDefinition.Format.RGBA8UI;
            case R16I -> FramebufferAttachmentDefinition.Format.R16I;
            case RG16I -> FramebufferAttachmentDefinition.Format.RG16I;
            case RGB16I -> FramebufferAttachmentDefinition.Format.RGB16I;
            case RGBA16I -> FramebufferAttachmentDefinition.Format.RGBA16I;
            case R16UI -> FramebufferAttachmentDefinition.Format.R16UI;
            case RG16UI -> FramebufferAttachmentDefinition.Format.RG16UI;
            case RGB16UI -> FramebufferAttachmentDefinition.Format.RGB16UI;
            case RGBA16UI -> FramebufferAttachmentDefinition.Format.RGBA16UI;
            case R32I -> FramebufferAttachmentDefinition.Format.R32I;
            case RG32I -> FramebufferAttachmentDefinition.Format.RG32I;
            case RGB32I -> FramebufferAttachmentDefinition.Format.RGB32I;
            case RGBA32I -> FramebufferAttachmentDefinition.Format.RGBA32I;
            case R32UI -> FramebufferAttachmentDefinition.Format.R32UI;
            case RG32UI -> FramebufferAttachmentDefinition.Format.RG32UI;
            case RGB32UI -> FramebufferAttachmentDefinition.Format.RGB32UI;
            case RGBA32UI -> FramebufferAttachmentDefinition.Format.RGBA32UI;
            case RGBA2 -> FramebufferAttachmentDefinition.Format.RGBA2;
            case RGBA4 -> FramebufferAttachmentDefinition.Format.RGBA4;
            case R3_G3_B2 -> FramebufferAttachmentDefinition.Format.R3_G3_B2;
            case RGB5_A1 -> FramebufferAttachmentDefinition.Format.RGB5_A1;
            case RGB565 -> FramebufferAttachmentDefinition.Format.RGB565;
            case RGB10_A2 -> FramebufferAttachmentDefinition.Format.RGB10_A2;
            case RGB10_A2UI -> FramebufferAttachmentDefinition.Format.RGB10_A2UI;
            case R11F_G11F_B10F -> FramebufferAttachmentDefinition.Format.R11F_G11F_B10F;
            case RGB9_E5 -> FramebufferAttachmentDefinition.Format.RGB9_E5;
        };
    }

    @Override
    public FramebufferAttachmentDefinition.DataType veil$getDataType() {
        return switch (this.type) {
            case BYTE -> FramebufferAttachmentDefinition.DataType.BYTE;
            case SHORT -> FramebufferAttachmentDefinition.DataType.SHORT;
            case INT -> FramebufferAttachmentDefinition.DataType.INT;
            case HALF_FLOAT -> FramebufferAttachmentDefinition.DataType.HALF_FLOAT;
            case FLOAT -> FramebufferAttachmentDefinition.DataType.FLOAT;
            case UNSIGNED_BYTE -> FramebufferAttachmentDefinition.DataType.UNSIGNED_BYTE;
            case UNSIGNED_BYTE_3_3_2 -> FramebufferAttachmentDefinition.DataType.UNSIGNED_BYTE_3_3_2;
            case UNSIGNED_BYTE_2_3_3_REV -> FramebufferAttachmentDefinition.DataType.UNSIGNED_BYTE_2_3_3_REV;
            case UNSIGNED_SHORT -> FramebufferAttachmentDefinition.DataType.UNSIGNED_SHORT;
            case UNSIGNED_SHORT_5_6_5 -> FramebufferAttachmentDefinition.DataType.UNSIGNED_SHORT_5_6_5;
            case UNSIGNED_SHORT_5_6_5_REV -> FramebufferAttachmentDefinition.DataType.UNSIGNED_SHORT_5_6_5_REV;
            case UNSIGNED_SHORT_4_4_4_4 -> FramebufferAttachmentDefinition.DataType.UNSIGNED_SHORT_4_4_4_4;
            case UNSIGNED_SHORT_4_4_4_4_REV -> FramebufferAttachmentDefinition.DataType.UNSIGNED_SHORT_4_4_4_4_REV;
            case UNSIGNED_SHORT_5_5_5_1 -> FramebufferAttachmentDefinition.DataType.UNSIGNED_SHORT_5_5_5_1;
            case UNSIGNED_SHORT_1_5_5_5_REV -> FramebufferAttachmentDefinition.DataType.UNSIGNED_SHORT_1_5_5_5_REV;
            case UNSIGNED_INT -> FramebufferAttachmentDefinition.DataType.UNSIGNED_INT;
            case UNSIGNED_INT_8_8_8_8 -> FramebufferAttachmentDefinition.DataType.UNSIGNED_INT_8_8_8_8;
            case UNSIGNED_INT_8_8_8_8_REV -> FramebufferAttachmentDefinition.DataType.UNSIGNED_INT_8_8_8_8_REV;
            case UNSIGNED_INT_10_10_10_2 -> FramebufferAttachmentDefinition.DataType.UNSIGNED_INT_10_10_10_2;
            case UNSIGNED_INT_2_10_10_10_REV -> FramebufferAttachmentDefinition.DataType.UNSIGNED_INT_2_10_10_10_REV;
            case UNSIGNED_INT_10F_11F_11F_REV -> FramebufferAttachmentDefinition.DataType.UNSIGNED_INT_10F_11F_11F_REV;
            case UNSIGNED_INT_5_9_9_9_REV -> FramebufferAttachmentDefinition.DataType.UNSIGNED_INT_5_9_9_9_REV;
        };
    }
}
