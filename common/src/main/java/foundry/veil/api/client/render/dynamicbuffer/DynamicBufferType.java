package foundry.veil.api.client.render.dynamicbuffer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import io.github.ocelot.glslprocessor.api.grammar.GlslTypeSpecifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Built-in dynamic buffers available to sample from.
 *
 * @author Ocelot
 */
public enum DynamicBufferType {
    ALBEDO("Albedo", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGBA8),
    NORMAL("Normal", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGB8_SNORM),
    LIGHT_UV("LightUV", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RG8),
    LIGHT_COLOR("LightColor", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGB8),
    DEBUG("Debug", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGBA16F);

    @ApiStatus.Internal
    public static final DynamicBufferType[] BUFFERS = values();
    private static final int MASK = (1 << BUFFERS.length) - 1;

    public static final Codec<DynamicBufferType> CODEC = Codec.STRING.flatXmap(name -> {
        for (DynamicBufferType buffer : BUFFERS) {
            if (buffer.getName().equals(name)) {
                return DataResult.success(buffer);
            }
        }
        return DataResult.error(() -> "Unknown dynamic buffer: " + name + ". Valid buffers: " + Arrays.stream(BUFFERS).map(DynamicBufferType::getName).collect(Collectors.joining(", ")));
    }, buffer -> DataResult.success(buffer.getName()));
    public static final Codec<Integer> PACKED_LIST_CODEC = CODEC.listOf().xmap(buffers -> {
        int mask = 0;
        for (DynamicBufferType buffer : buffers) {
            mask |= buffer.mask;
        }
        return mask;
    }, mask -> Arrays.asList(decode(mask)));

    private final String name;
    private final String sourceName;
    private final GlslTypeSpecifier.BuiltinType type;
    private final int internalFormat;
    private final int texelFormat;
    private final int mask;

    DynamicBufferType(String sourceName, GlslTypeSpecifier.BuiltinType type, FramebufferAttachmentDefinition.Format format) {
        this.name = this.name().toLowerCase(Locale.ROOT);
        this.sourceName = "VeilDynamic" + sourceName;
        this.type = type;
        this.internalFormat = format.getInternalFormat();
        this.texelFormat = format.getFormat();
        this.mask = 1 << this.ordinal();
    }

    /**
     * @return The code name of this type
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return The name of this type inside the GLSL file
     */
    public String getSourceName() {
        return this.sourceName;
    }

    /**
     * @return The data type stored in this buffer
     */
    public GlslTypeSpecifier.BuiltinType getType() {
        return this.type;
    }

    /**
     * @return The internal format of the texture
     */
    public int getInternalFormat() {
        return this.internalFormat;
    }

    /**
     * @return The format of data assigned to the texture
     */
    public int getTexelFormat() {
        return this.texelFormat;
    }

    /**
     * @return The bit mask of this type
     */
    public int getMask() {
        return this.mask;
    }

    /**
     * Adds the standard buffer macros to the specified map.
     *
     * @param mask The mask of enabled buffers
     * @param map  The map to add macros to
     */
    public static void addMacros(int mask, Map<String, String> map) {
        for (DynamicBufferType value : BUFFERS) {
            if ((value.mask & mask) != 0) {
                map.put("VEIL_" + value.name(), "1");
            }
        }
    }

    /**
     * Encodes the specified dynamic buffer types as a bit mask.
     *
     * @param types The types to encode
     * @return An integer representing those buffer types
     */
    public static int encode(DynamicBufferType... types) {
        int mask = 0;
        for (DynamicBufferType type : types) {
            mask |= type.mask;
        }
        return mask;
    }

    /**
     * Decodes the dynamic buffer types from the specified mask.
     *
     * @param mask The mask of buffers
     * @return An array of all decoded buffers
     */
    public static DynamicBufferType[] decode(int mask) {
        int next = 0;
        DynamicBufferType[] types = new DynamicBufferType[Integer.bitCount(mask & MASK)];
        for (DynamicBufferType value : BUFFERS) {
            if ((value.mask & mask) != 0) {
                types[next++] = value;
            }
        }
        return types;
    }
}
