package foundry.veil.api.client.render.texture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.util.ColorCodec;
import foundry.veil.api.util.EnumCodec;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Optional;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.ARBTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13C.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL14C.*;
import static org.lwjgl.opengl.GL30C.GL_COMPARE_REF_TO_TEXTURE;
import static org.lwjgl.opengl.GL32C.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static org.lwjgl.opengl.GL33C.glTexParameterIiv;
import static org.lwjgl.opengl.GL33C.glTexParameterIuiv;
import static org.lwjgl.opengl.GL44C.GL_MIRROR_CLAMP_TO_EDGE;

/**
 * Specifies the full texture filter state of a texture or sampler object.
 *
 * @param blur            Whether to blur the image
 * @param mipmap          Whether to respect the mipmap levels of the image
 * @param anisotropy      Sets the anisotropic filtering value. Any value >1 is considered to be enabled. Set to {@link Float#MAX_VALUE} to set to the platform maximum
 * @param compareFunction The depth compare function to use or <code>null</code> for default
 * @param wrapX           The clamping for the X axis on the texture
 * @param wrapY           The clamping for the Y axis on the texture
 * @param wrapZ           The clamping for the Z axis on the texture
 * @param edgeColor       The color to get when sampling the texture out of bounds when using {@link TextureFilter.Wrap#CLAMP_TO_BORDER}
 * @param edgeType        The type of data the edge color should be referenced in
 * @param seamless        Whether the texture should be considered seamless if using a cubemap
 * @author Ocelot
 */
public record TextureFilter(boolean blur,
                            boolean mipmap,
                            float anisotropy,
                            @Nullable CompareFunction compareFunction,
                            Wrap wrapX,
                            Wrap wrapY,
                            Wrap wrapZ,
                            int edgeColor,
                            EdgeType edgeType,
                            boolean seamless) {

    public static final Codec<TextureFilter> REPEAT_DEFAULT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("blur", false).forGetter(TextureFilter::blur),
            Codec.BOOL.optionalFieldOf("mipmap", false).forGetter(TextureFilter::mipmap),
            Codec.FLOAT.optionalFieldOf("anisotropy", 1.0F).forGetter(TextureFilter::anisotropy),
            CompareFunction.CODEC.optionalFieldOf("compareFunction").forGetter(options -> Optional.ofNullable(options.compareFunction())),
            Wrap.CODEC.optionalFieldOf("wrapX", Wrap.REPEAT).forGetter(TextureFilter::wrapX),
            Wrap.CODEC.optionalFieldOf("wrapY", Wrap.REPEAT).forGetter(TextureFilter::wrapY),
            Wrap.CODEC.optionalFieldOf("wrapZ", Wrap.REPEAT).forGetter(TextureFilter::wrapZ),
            ColorCodec.ARGB_CODEC.optionalFieldOf("edgeColor", 0xFF000000).forGetter(TextureFilter::edgeColor),
            EdgeType.CODEC.optionalFieldOf("edgeType", EdgeType.FLOAT).forGetter(TextureFilter::edgeType),
            Codec.BOOL.optionalFieldOf("seamless", false).forGetter(TextureFilter::seamless)
    ).apply(instance, (blur, mipmap, anisotropy, compareFunction, wrapX, wrapY, wrapZ, edgeColor, edgeType, seamless) ->
            new TextureFilter(blur, mipmap, anisotropy, compareFunction.orElse(null), wrapX, wrapY, wrapZ, edgeColor, edgeType, seamless)));

    public static final Codec<TextureFilter> CLAMP_DEFAULT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("blur", false).forGetter(TextureFilter::blur),
            Codec.BOOL.optionalFieldOf("mipmap", false).forGetter(TextureFilter::mipmap),
            Codec.FLOAT.optionalFieldOf("anisotropy", 1.0F).forGetter(TextureFilter::anisotropy),
            CompareFunction.CODEC.optionalFieldOf("compareFunction").forGetter(options -> Optional.ofNullable(options.compareFunction())),
            Wrap.CODEC.optionalFieldOf("wrapX", Wrap.CLAMP_TO_EDGE).forGetter(TextureFilter::wrapX),
            Wrap.CODEC.optionalFieldOf("wrapY", Wrap.CLAMP_TO_EDGE).forGetter(TextureFilter::wrapY),
            Wrap.CODEC.optionalFieldOf("wrapZ", Wrap.CLAMP_TO_EDGE).forGetter(TextureFilter::wrapZ),
            ColorCodec.ARGB_CODEC.optionalFieldOf("edgeColor", -1).forGetter(TextureFilter::edgeColor),
            EdgeType.CODEC.optionalFieldOf("edgeType", EdgeType.FLOAT).forGetter(TextureFilter::edgeType),
            Codec.BOOL.optionalFieldOf("seamless", false).forGetter(TextureFilter::seamless)
    ).apply(instance, (blur, mipmap, anisotropy, compareFunction, wrapX, wrapY, wrapZ, edgeColor, edgeType, seamless) ->
            new TextureFilter(blur, mipmap, anisotropy, compareFunction.orElse(null), wrapX, wrapY, wrapZ, edgeColor, edgeType, seamless)));

    public static final TextureFilter REPEAT = new TextureFilter(
            false,
            false,
            1.0F,
            null,
            Wrap.REPEAT,
            Wrap.REPEAT,
            Wrap.REPEAT,
            -1,
            EdgeType.FLOAT,
            false);
    public static final TextureFilter CLAMP = new TextureFilter(
            false,
            false,
            1.0F,
            null,
            Wrap.CLAMP_TO_EDGE,
            Wrap.CLAMP_TO_EDGE,
            Wrap.CLAMP_TO_EDGE,
            -1,
            EdgeType.FLOAT,
            false);

    /**
     * @return The OpenGl filter to use when making the image smaller (minification)
     */
    public int minFilter() {
        if (this.blur) {
            return this.mipmap ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR;
        } else {
            return this.mipmap ? GL_NEAREST_MIPMAP_LINEAR : GL_NEAREST;
        }
    }

    /**
     * @return The OpenGl filter to use when making the image larger (magnification)
     */
    public int magFilter() {
        return this.blur ? GL_LINEAR : GL_NEAREST;
    }

    /**
     * Applies the filtering options to the specified texture target.
     *
     * @param target The target to apply filtering to
     */
    public void applyToTextureTarget(int target) {
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, this.minFilter());
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, this.magFilter());

        if (VeilRenderSystem.textureAnisotropySupported()) {
            glTexParameterf(target, GL_TEXTURE_MAX_ANISOTROPY, Math.min(this.anisotropy, VeilRenderSystem.maxTextureAnisotropy()));
        }

        if (this.compareFunction != null) {
            glTexParameteri(target, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
            glTexParameteri(target, GL_TEXTURE_COMPARE_FUNC, this.compareFunction.id);
        } else {
            glTexParameteri(target, GL_TEXTURE_COMPARE_MODE, GL_NONE);
        }

        glTexParameteri(target, GL_TEXTURE_WRAP_S, this.wrapX.id);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, this.wrapY.id);
        glTexParameteri(target, GL_TEXTURE_WRAP_R, this.wrapZ.id);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer data = stack.ints((this.edgeColor >> 16) & 0xFF, (this.edgeColor >> 8) & 0xFF, this.edgeColor & 0xFF, (this.edgeColor >> 24) & 0xFF);
            switch (this.edgeType) {
                case FLOAT -> glTexParameteriv(target, GL_TEXTURE_BORDER_COLOR, data);
                case INT -> glTexParameterIiv(target, GL_TEXTURE_BORDER_COLOR, data);
                case UINT -> glTexParameterIuiv(target, GL_TEXTURE_BORDER_COLOR, data);
            }
        }

        if (VeilRenderSystem.textureCubeMapSeamlessSupported()) {
            glTexParameteri(target, GL_TEXTURE_CUBE_MAP_SEAMLESS, this.seamless ? 1 : 0);
        }
    }

    /**
     * Applies the filtering options to the specified texture object. Only valid if {@link VeilRenderSystem#directStateAccessSupported()} is <code>true</code>.
     *
     * @param texture The texture to apply filtering to
     */
    public void applyToTexture(int texture) {
        glTextureParameteri(texture, GL_TEXTURE_MIN_FILTER, this.minFilter());
        glTextureParameteri(texture, GL_TEXTURE_MAG_FILTER, this.magFilter());

        if (VeilRenderSystem.textureAnisotropySupported()) {
            glTextureParameterf(texture, GL_TEXTURE_MAX_ANISOTROPY, Math.min(this.anisotropy, VeilRenderSystem.maxTextureAnisotropy()));
        }

        if (this.compareFunction != null) {
            glTextureParameteri(texture, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
            glTextureParameteri(texture, GL_TEXTURE_COMPARE_FUNC, this.compareFunction.id);
        } else {
            glTextureParameteri(texture, GL_TEXTURE_COMPARE_MODE, GL_NONE);
        }

        glTextureParameteri(texture, GL_TEXTURE_WRAP_S, this.wrapX.id);
        glTextureParameteri(texture, GL_TEXTURE_WRAP_T, this.wrapY.id);
        glTextureParameteri(texture, GL_TEXTURE_WRAP_R, this.wrapZ.id);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer data = stack.ints((this.edgeColor >> 16) & 0xFF, (this.edgeColor >> 8) & 0xFF, this.edgeColor & 0xFF, (this.edgeColor >> 24) & 0xFF);
            switch (this.edgeType) {
                case FLOAT -> glTextureParameteriv(texture, GL_TEXTURE_BORDER_COLOR, data);
                case INT -> glTextureParameterIiv(texture, GL_TEXTURE_BORDER_COLOR, data);
                case UINT -> glTextureParameterIuiv(texture, GL_TEXTURE_BORDER_COLOR, data);
            }
        }

        if (VeilRenderSystem.textureCubeMapSeamlessSupported()) {
            glTextureParameteri(texture, GL_TEXTURE_CUBE_MAP_SEAMLESS, this.seamless ? 1 : 0);
        }
    }

    /**
     * Depth-texture compare functions.
     */
    public enum CompareFunction {
        NEVER(GL_NEVER),
        ALWAYS(GL_ALWAYS),
        LESS(GL_LESS),
        LEQUAL(GL_LEQUAL),
        EQUAL(GL_EQUAL),
        NOT_EQUAL(GL_NOTEQUAL),
        GEQUAL(GL_GEQUAL),
        GREATER(GL_GREATER);

        public static final Codec<CompareFunction> CODEC = EnumCodec.<CompareFunction>builder("Compare Function")
                .values(CompareFunction.class)
                .build();

        private final int id;

        CompareFunction(int id) {
            this.id = id;
        }

        /**
         * @return The OpenGL id of this compare function
         */
        public int getId() {
            return this.id;
        }
    }

    /**
     * Texture wrap modes.
     */
    public enum Wrap {
        CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE),
        CLAMP_TO_BORDER(GL_CLAMP_TO_BORDER),
        MIRRORED_REPEAT(GL_MIRRORED_REPEAT),
        REPEAT(GL_REPEAT),
        MIRROR_CLAMP_TO_EDGE(GL_MIRROR_CLAMP_TO_EDGE);

        public static final Codec<Wrap> CODEC = EnumCodec.<Wrap>builder("Texture Wrap")
                .values(Wrap.class)
                .build();
        public static final Int2ObjectMap<Wrap> BY_GL_ID;

        static {
            Int2ObjectMap<Wrap> map = new Int2ObjectArrayMap<>();
            for (Wrap filter : values()) {
                map.put(filter.id, filter);
            }
            BY_GL_ID = Int2ObjectMaps.unmodifiable(map);
        }

        private final int id;

        Wrap(int id) {
            this.id = id;
        }

        /**
         * @return The OpenGL id of this wrap mode
         */
        public int getId() {
            return this == MIRROR_CLAMP_TO_EDGE && !VeilRenderSystem.textureMirrorClampToEdgeSupported() ? GL_CLAMP_TO_EDGE : this.id;
        }
    }

    /**
     * Edge color data types.
     */
    public enum EdgeType {
        FLOAT, INT, UINT;

        public static final Codec<EdgeType> CODEC = EnumCodec.<EdgeType>builder("Edge Data Type")
                .values(EdgeType.class)
                .build();
    }
}
