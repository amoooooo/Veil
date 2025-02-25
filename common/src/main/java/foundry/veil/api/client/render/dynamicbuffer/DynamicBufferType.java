package foundry.veil.api.client.render.dynamicbuffer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import io.github.ocelot.glslprocessor.api.grammar.GlslTypeSpecifier;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public enum DynamicBufferType {
    ALBEDO("Albedo", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGBA8),
    NORMAL("Normal", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGB8_SNORM),
    LIGHT_UV("LightUV", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RG8),
    LIGHT_COLOR("LightColor", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGB8),
    DEBUG("Debug", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGBA16F);

    private static final DynamicBufferType[] BUFFERS = values();

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

    public String getName() {
        return this.name;
    }

    public String getSourceName() {
        return this.sourceName;
    }

    public GlslTypeSpecifier.BuiltinType getType() {
        return this.type;
    }

    public int getInternalFormat() {
        return this.internalFormat;
    }

    public int getTexelFormat() {
        return this.texelFormat;
    }

    public int getMask() {
        return this.mask;
    }

    public static void addMacros(int mask, Map<String, String> map) {
        for (DynamicBufferType value : BUFFERS) {
            if ((value.mask & mask) != 0) {
                map.put("VEIL_" + value.name(), "1");
            }
        }
    }

    public static int encode(DynamicBufferType... types) {
        int mask = 0;
        for (DynamicBufferType type : types) {
            mask |= type.mask;
        }
        return mask;
    }

    public static DynamicBufferType[] decode(int mask) {
        int next = 0;
        DynamicBufferType[] types = new DynamicBufferType[Integer.bitCount(mask)];
        for (DynamicBufferType value : BUFFERS) {
            if ((value.mask & mask) != 0) {
                types[next++] = value;
            }
        }
        return types;
    }
}
