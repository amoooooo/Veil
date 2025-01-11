package foundry.veil.api.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.util.EnumCodec;

import java.util.Locale;
import java.util.Map;

public class BlendState {

    public static final BlendState NONE = new BlendState(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ZERO) {
        @Override
        public void setup() {
            RenderSystem.disableBlend();
        }

        @Override
        public void clear() {
        }
    };
    public static final BlendState ADDITIVE = new BlendState(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
    public static final BlendState LIGHTNING = new BlendState(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    public static final BlendState GLINT = new BlendState(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
    public static final BlendState CRUMBLING = new BlendState(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    public static final BlendState TRANSLUCENT = new BlendState(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

    private static final Map<String, BlendState> VANILLA_BLEND_STATES = Map.of(
            "NONE", NONE,
            "ADDITIVE", ADDITIVE,
            "LIGHTNING", LIGHTNING,
            "GLINT", GLINT,
            "CRUMBLING", CRUMBLING,
            "TRANSLUCENT", TRANSLUCENT
    );
    private static final Codec<GlStateManager.SourceFactor> SOURCE_CODEC = EnumCodec.<GlStateManager.SourceFactor>builder("Source Factor").uppercase().values(GlStateManager.SourceFactor.class).build();
    private static final Codec<GlStateManager.DestFactor> DESTINATION_CODEC = EnumCodec.<GlStateManager.DestFactor>builder("Destination Factor").uppercase().values(GlStateManager.DestFactor.class).build();
    private static final Codec<BlendState> DEFAULT_CODEC = Codec.STRING.flatXmap(name -> {
        String key = name.toUpperCase(Locale.ROOT);
        BlendState state = VANILLA_BLEND_STATES.get(key);
        return state != null ? DataResult.success(state) : DataResult.error(() -> "Unknown default blend state: " + key);
    }, state -> {
        for (Map.Entry<String, BlendState> entry : VANILLA_BLEND_STATES.entrySet()) {
            if (entry.getValue().equals(state)) {
                return DataResult.success(entry.getKey());
            }
        }
        return DataResult.error(() -> "Unknown default blend state for: " + state);
    });
    private static final Codec<BlendState> COMPRESSED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SOURCE_CODEC.fieldOf("src").forGetter(BlendState::src),
            DESTINATION_CODEC.fieldOf("dest").forGetter(BlendState::dst)
    ).apply(instance, BlendState::new));
    private static final Codec<BlendState> FULL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SOURCE_CODEC.fieldOf("src_color").forGetter(BlendState::src),
            DESTINATION_CODEC.fieldOf("dest_color").forGetter(BlendState::dst),
            SOURCE_CODEC.fieldOf("src_alpha").forGetter(BlendState::src),
            DESTINATION_CODEC.fieldOf("dest_alpha").forGetter(BlendState::dst)
    ).apply(instance, BlendState::new));
    private static final Codec<BlendState> A = Codec.either(FULL_CODEC, COMPRESSED_CODEC)
            .xmap(either -> either.map(s -> s, s -> s),
                    state -> state.srcColor == state.srcAlpha && state.dstColor == state.dstAlpha ? Either.right(state) : Either.left(state));

    public static final Codec<BlendState> CODEC = Codec.either(FULL_CODEC, COMPRESSED_CODEC)
            .xmap(either -> either.map(s -> s, s -> s),
                    state -> state.srcColor == state.srcAlpha && state.dstColor == state.dstAlpha ? Either.right(state) : Either.left(state));

    private final GlStateManager.SourceFactor srcColor;
    private final GlStateManager.DestFactor dstColor;
    private final GlStateManager.SourceFactor srcAlpha;
    private final GlStateManager.DestFactor dstAlpha;

    public BlendState(GlStateManager.SourceFactor srcColor, GlStateManager.DestFactor dstColor, GlStateManager.SourceFactor srcAlpha, GlStateManager.DestFactor dstAlpha) {
        this.srcColor = srcColor;
        this.dstColor = dstColor;
        this.srcAlpha = srcAlpha;
        this.dstAlpha = dstAlpha;
    }

    public BlendState(GlStateManager.SourceFactor src, GlStateManager.DestFactor dst) {
        this(src, dst, src, dst);
    }

    public void setup() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(this.srcColor, this.dstColor, this.srcAlpha, this.dstAlpha);
    }

    public void clear() {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public GlStateManager.SourceFactor src() {
        return this.srcColor;
    }

    public GlStateManager.DestFactor dst() {
        return this.dstColor;
    }

    public GlStateManager.SourceFactor srcAlpha() {
        return this.srcAlpha;
    }

    public GlStateManager.DestFactor dstAlpha() {
        return this.dstAlpha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        BlendState state = (BlendState) o;
        if (this.srcColor != state.srcColor) {
            return false;
        }
        if (this.dstColor != state.dstColor) {
            return false;
        }
        if (this.srcAlpha != state.srcAlpha) {
            return false;
        }
        return this.dstAlpha == state.dstAlpha;
    }

    @Override
    public int hashCode() {
        int result = this.srcColor.hashCode();
        result = 31 * result + this.dstColor.hashCode();
        result = 31 * result + this.srcAlpha.hashCode();
        result = 31 * result + this.dstAlpha.hashCode();
        return result;
    }
}
