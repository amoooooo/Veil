package foundry.veil.api.client.render.framebuffer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.render.post.PostProcessingManager;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL14C.*;
import static org.lwjgl.opengl.GL21C.GL_SRGB8;
import static org.lwjgl.opengl.GL21C.GL_SRGB8_ALPHA8;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL31C.*;
import static org.lwjgl.opengl.GL33C.GL_RGB10_A2UI;
import static org.lwjgl.opengl.GL41C.GL_RGB565;

/**
 * Represents a framebuffer attachment that can be turned into a real framebuffer.
 *
 * @param type   The type of attachment this is
 * @param format The internal format of the data
 * @param depth  Whether this is a color or depth attachment
 * @param linear Whether this should have linear filtering. Only applies to texture buffers
 * @param levels The number of mipmaps for textures and samples for render buffers
 * @param name   The custom name to use when uploading this as a sampler to shaders
 * @author Ocelot
 * @see AdvancedFbo
 * @see FramebufferManager
 * @see PostProcessingManager
 */
public record FramebufferAttachmentDefinition(FramebufferAttachmentDefinition.Type type,
                                              FramebufferAttachmentDefinition.Format format,
                                              FramebufferAttachmentDefinition.DataType dataType,
                                              boolean depth,
                                              boolean linear,
                                              int levels,
                                              @Nullable String name) {

    public static final Codec<FramebufferAttachmentDefinition> COLOR_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Type.CODEC.optionalFieldOf("type", Type.TEXTURE)
                            .forGetter(FramebufferAttachmentDefinition::type),
                    Format.CODEC.optionalFieldOf("format", Format.RGBA8)
                            .forGetter(FramebufferAttachmentDefinition::format),
                    DataType.CODEC.optionalFieldOf("dataType", DataType.UNSIGNED_BYTE)
                            .forGetter(FramebufferAttachmentDefinition::dataType),
                    Codec.BOOL.optionalFieldOf("linear", false)
                            .forGetter(FramebufferAttachmentDefinition::linear),
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("levels", 1)
                            .forGetter(FramebufferAttachmentDefinition::levels),
                    Codec.STRING.optionalFieldOf("name")
                            .forGetter(attachment -> Optional.ofNullable(attachment.name()))
            ).apply(instance, (type, format, dataType, linear, levels, name) ->
                    new FramebufferAttachmentDefinition(type, format, dataType, false, linear, levels, name.orElse(null))));
    public static final Codec<FramebufferAttachmentDefinition> DEPTH_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Type.CODEC.optionalFieldOf("type", Type.TEXTURE)
                            .forGetter(FramebufferAttachmentDefinition::type),
                    Format.CODEC.optionalFieldOf("format", Format.DEPTH_COMPONENT)
                            .forGetter(FramebufferAttachmentDefinition::format),
                    DataType.CODEC.optionalFieldOf("dataType", DataType.FLOAT)
                            .forGetter(FramebufferAttachmentDefinition::dataType),
                    Codec.BOOL.optionalFieldOf("linear", false)
                            .forGetter(FramebufferAttachmentDefinition::linear),
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("levels", 1)
                            .forGetter(FramebufferAttachmentDefinition::levels),
                    Codec.STRING.optionalFieldOf("name")
                            .forGetter(attachment -> Optional.ofNullable(attachment.name()))
            ).apply(instance, (type, format, dataType, linear, levels, name) ->
                    new FramebufferAttachmentDefinition(type, format, dataType, true, linear, levels, name.orElse(null))));

    /**
     * @return Whether this attachment can be represented as <code>"depth": true</code> in the JSON
     */
    public boolean isCompactDepthAttachment() {
        return this.type == Type.TEXTURE && this.format == Format.DEPTH_COMPONENT && this.dataType == DataType.FLOAT && !this.linear && this.levels == 1 && this.name == null;
    }

    /**
     * The type of attachments.
     */
    public enum Type {

        TEXTURE("Texture"),
        RENDER_BUFFER("Render Buffer");

        public static final Codec<Type> CODEC = Codec.STRING.flatXmap(name -> {
            for (Type type : Type.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return DataResult.success(type);
                }
            }
            return DataResult.error(() -> "Unknown attachment type: " + name);
        }, type -> DataResult.success(type.name()));

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * The formats for attachments.
     */
    public enum Format {

        RED(GL_RED, GL_RED),
        RG(GL_RG, GL_RG),
        RGB(GL_RGB, GL_RGB),
        BGR(GL_BGR, GL_BGR),
        RGBA(GL_RGBA, GL_RGBA),
        BGRA(GL_BGRA, GL_BGRA),
        DEPTH_COMPONENT(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT),
        DEPTH_STENCIL(GL_DEPTH_STENCIL, GL_DEPTH_STENCIL),
        R8(GL_RED, GL_R8),
        R8_SNORM(GL_RED, GL_R8_SNORM),
        R16(GL_RED, GL_R16),
        R16_SNORM(GL_RED, GL_R16_SNORM),
        RG8(GL_RG, GL_RG8),
        RG8_SNORM(GL_RG, GL_RG8_SNORM),
        RG16(GL_RG, GL_RG16),
        RG16_SNORM(GL_RG, GL_RG16_SNORM),
        R3_G3_B2(GL_RGB, GL_R3_G3_B2),
        RGB4(GL_RGB, GL_RGB4),
        RGB5(GL_RGB, GL_RGB5),
        RGB565(GL_RGB, GL_RGB565),
        RGB8(GL_RGB, GL_RGB8),
        RGB8_SNORM(GL_RGB, GL_RGB8_SNORM),
        RGB10(GL_RGB, GL_RGB10),
        RGB12(GL_RGB, GL_RGB12),
        RGB16(GL_RGB, GL_RGB16),
        RGB16_SNORM(GL_RGB, GL_RGB16_SNORM),
        RGBA2(GL_RGBA, GL_RGBA2),
        RGBA4(GL_RGBA, GL_RGBA4),
        RGB5_A1(GL_RGBA, GL_RGB5_A1),
        RGBA8(GL_RGBA, GL_RGBA8),
        RGBA8_SNORM(GL_RGBA, GL_RGBA8_SNORM),
        RGB10_A2(GL_RGBA, GL_RGB10_A2),
        RGB10_A2UI(GL_RGBA, GL_RGB10_A2UI),
        RGBA12(GL_RGBA, GL_RGBA12),
        RGBA16(GL_RGBA, GL_RGBA16),
        RGBA16_SNORM(GL_RGBA, GL_RGBA16_SNORM),
        SRGB(GL_RGB, GL_SRGB),
        SRGB8(GL_RGB, GL_SRGB8),
        SRGB_ALPHA(GL_RGBA, GL_SRGB_ALPHA),
        SRGB8_ALPHA8(GL_RGBA, GL_SRGB8_ALPHA8),
        COMPRESSED_SRGB(GL_RGB, GL_COMPRESSED_SRGB),
        COMPRESSED_SRGB_ALPHA(GL_RGBA, GL_COMPRESSED_SRGB_ALPHA),
        R16F(GL_RED, GL_R16F),
        RG16F(GL_RG, GL_RG16F),
        RGB16F(GL_RGB, GL_RGB16F),
        RGBA16F(GL_RGBA, GL_RGBA16F),
        R32F(GL_RED, GL_R32F),
        RG32F(GL_RG, GL_R32F),
        RGB32F(GL_RGB, GL_RGB32F),
        RGB9_E5(GL_RGB, GL_RGB9_E5),
        RGBA32F(GL_RGBA, GL_RGBA32F),
        R11F_G11F_B10F(GL_RGBA, GL_R11F_G11F_B10F),
        R8I(GL_RED_INTEGER, GL_R8I),
        R8UI(GL_RED_INTEGER, GL_R8UI),
        R16I(GL_RED_INTEGER, GL_R16I),
        R16UI(GL_RED_INTEGER, GL_R16UI),
        R32I(GL_RED_INTEGER, GL_R32I),
        R32UI(GL_RED_INTEGER, GL_R32UI),
        RG8I(GL_RG_INTEGER, GL_RG8I),
        RG8UI(GL_RG_INTEGER, GL_RG8UI),
        RG16I(GL_RG_INTEGER, GL_RG16I),
        RG16UI(GL_RG_INTEGER, GL_RG16UI),
        RG32I(GL_RG_INTEGER, GL_RG32I),
        RG32UI(GL_RG_INTEGER, GL_RG32UI),
        RGB8I(GL_RGB_INTEGER, GL_RGB8I),
        RGB8UI(GL_RGB_INTEGER, GL_RGB8UI),
        RGB16I(GL_RGB_INTEGER, GL_RGB16I),
        RGB16UI(GL_RGB_INTEGER, GL_RGB16UI),
        RGB32I(GL_RGB_INTEGER, GL_RGB32I),
        RGB32UI(GL_RGB_INTEGER, GL_RGB32UI),
        RGBA8I(GL_RGBA_INTEGER, GL_RGBA8I),
        RGBA8UI(GL_RGBA_INTEGER, GL_RGBA8UI),
        RGBA16I(GL_RGBA_INTEGER, GL_RGBA16I),
        RGBA16UI(GL_RGBA_INTEGER, GL_RGBA16UI),
        RGBA32I(GL_RGBA_INTEGER, GL_RGBA32I),
        RGBA32UI(GL_RGBA_INTEGER, GL_RGBA32UI),
        DEPTH_COMPONENT16(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT16),
        DEPTH_COMPONENT24(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT24),
        DEPTH_COMPONENT32(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT32),
        DEPTH_COMPONENT32F(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT32F),
        DEPTH24_STENCIL8(GL_DEPTH_STENCIL, GL_DEPTH24_STENCIL8),
        DEPTH32F_STENCIL8(GL_DEPTH_STENCIL, GL_DEPTH32F_STENCIL8);

        public static final Codec<Format> CODEC = Codec.STRING.flatXmap(name -> {
            for (Format type : Format.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return DataResult.success(type);
                }
            }
            return DataResult.error(() -> "Unknown attachment format: " + name);
        }, type -> DataResult.success(type.name()));

        private final int id;
        private final int internalId;

        Format(int id, int internalId) {
            this.id = id;
            this.internalId = internalId;
        }

        /**
         * @return The OpenGL id of this format
         */
        public int getId() {
            return this.id;
        }

        /**
         * @return The OpenGL id of this internal format
         */
        public int getInternalId() {
            return this.internalId;
        }
    }

    /**
     * The formats for attachments.
     */
    public enum DataType {

        UNSIGNED_BYTE(GL_UNSIGNED_BYTE),
        BYTE(GL_BYTE),
        UNSIGNED_SHORT(GL_UNSIGNED_SHORT),
        SHORT(GL_SHORT),
        UNSIGNED_INT(GL_UNSIGNED_INT),
        INT(GL_INT),
        HALF_FLOAT(GL_HALF_FLOAT),
        FLOAT(GL_FLOAT),
        UNSIGNED_BYTE_3_3_2(GL_UNSIGNED_BYTE_3_3_2),
        UNSIGNED_BYTE_2_3_3_REV(GL_UNSIGNED_BYTE_2_3_3_REV),
        UNSIGNED_SHORT_5_6_5(GL_UNSIGNED_SHORT_5_6_5),
        UNSIGNED_SHORT_5_6_5_REV(GL_UNSIGNED_SHORT_5_6_5_REV),
        UNSIGNED_SHORT_4_4_4_4(GL_UNSIGNED_SHORT_4_4_4_4),
        UNSIGNED_SHORT_4_4_4_4_REV(GL_UNSIGNED_SHORT_4_4_4_4_REV),
        UNSIGNED_SHORT_5_5_5_1(GL_UNSIGNED_SHORT_5_5_5_1),
        UNSIGNED_SHORT_1_5_5_5_REV(GL_UNSIGNED_SHORT_1_5_5_5_REV),
        UNSIGNED_INT_8_8_8_8(GL_UNSIGNED_INT_8_8_8_8),
        UNSIGNED_INT_8_8_8_8_REV(GL_UNSIGNED_INT_8_8_8_8_REV),
        UNSIGNED_INT_10_10_10_2(GL_UNSIGNED_INT_10_10_10_2),
        UNSIGNED_INT_2_10_10_10_REV(GL_UNSIGNED_INT_2_10_10_10_REV),
        UNSIGNED_INT_24_8(GL_UNSIGNED_INT_24_8),
        UNSIGNED_INT_10F_11F_11F_REV(GL_UNSIGNED_INT_10F_11F_11F_REV),
        UNSIGNED_INT_5_9_9_9_REV(GL_UNSIGNED_INT_5_9_9_9_REV),
        FLOAT_32_UNSIGNED_INT_24_8_REV(GL_FLOAT_32_UNSIGNED_INT_24_8_REV);

        public static final Codec<DataType> CODEC = Codec.STRING.flatXmap(name -> {
            for (DataType type : DataType.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return DataResult.success(type);
                }
            }
            return DataResult.error(() -> "Unknown attachment data type: " + name);
        }, type -> DataResult.success(type.name()));

        private final int id;

        DataType(int id) {
            this.id = id;
        }

        /**
         * @return The OpenGL id of this data type
         */
        public int getId() {
            return this.id;
        }
    }
}
