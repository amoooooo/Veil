package foundry.veil.api.client.render.dynamicbuffer;

import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.impl.glsl.grammar.GlslTypeSpecifier;

public enum DynamicBufferType {
    ALBEDO("albedo", "Albedo", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGBA8),
    NORMAL("normal", "Normal", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGB8_SNORM),
    LIGHT_UV("light_uv", "LightUV", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RG8),
    LIGHT_COLOR("light_color", "LightColor", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGB8),
    DEBUG("debug", "Debug", GlslTypeSpecifier.BuiltinType.VEC4, FramebufferAttachmentDefinition.Format.RGBA16F);

    private final String name;
    private final String sourceName;
    private final GlslTypeSpecifier.BuiltinType type;
    private final int internalFormat;
    private final int texelFormat;
    private final int mask;

    DynamicBufferType(String name, String sourceName, GlslTypeSpecifier.BuiltinType type, FramebufferAttachmentDefinition.Format format) {
        this.name = name;
        this.sourceName = "VeilDynamic" + sourceName;
        this.type=type;
        this.internalFormat = format.getInternalId();
        this.texelFormat = format.getId();
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
        for (DynamicBufferType value : values()) {
            if ((value.mask & mask) != 0) {
                types[next++] = value;
            }
        }
        return types;
    }
}
