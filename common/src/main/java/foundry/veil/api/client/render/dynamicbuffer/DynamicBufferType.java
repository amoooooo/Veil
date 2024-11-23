package foundry.veil.api.client.render.dynamicbuffer;

import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;

public enum DynamicBufferType {
    ALBEDO("albedo", "Albedo", false, 4, FramebufferAttachmentDefinition.Format.RGBA8),
    NORMAL("normal", "Normal", false, 3, FramebufferAttachmentDefinition.Format.RGB8_SNORM),
    LIGHT_COLOR("light_color", "LightColor", false, 3, FramebufferAttachmentDefinition.Format.RGB8),
    LIGHT_UV("light_uv", "LightUv", true, 2, FramebufferAttachmentDefinition.Format.RG8UI),
    DEBUG("debug", "Debug", false, 4, FramebufferAttachmentDefinition.Format.RGBA16F);

    private final String name;
    private final String sourceName;
    private final boolean integer;
    private final int components;
    private final int internalFormat;
    private final int texelFormat;
    private final int mask;

    DynamicBufferType(String name, String sourceName, boolean integer, int components, FramebufferAttachmentDefinition.Format format) {
        this.name = name;
        this.sourceName = "VeilDynamic" + sourceName;
        this.integer = integer;
        this.components = components;
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

    public String getType() {
        if (this.components == 1) {
            return this.isInteger() ? "int" : "float";
        }
        return (this.isInteger() ? "u" : "") + "vec" + this.components;
    }

    public boolean isInteger() {
        return this.integer;
    }

    public int getComponents() {
        return this.components;
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
