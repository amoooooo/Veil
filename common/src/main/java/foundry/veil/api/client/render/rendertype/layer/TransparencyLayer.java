package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.renderer.RenderStateShard;

public record TransparencyLayer(LayerTemplateValue<BlendState> mode) implements RenderTypeLayer {

    public static final MapCodec<TransparencyLayer> CODEC = LayerTemplateValue.enumCodec(BlendState.class)
            .fieldOf("mode")
            .xmap(TransparencyLayer::new, TransparencyLayer::mode);

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        builder.transparencyState(this.mode.parse(params).shard);
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.TRANSPARENCY.get();
    }

    public enum BlendState {
        NONE(VeilRenderType.noTransparencyShard()),
        ADDITIVE(VeilRenderType.additiveTransparencyShard()),
        LIGHTNING(VeilRenderType.lightningTransparencyShard()),
        GLINT(VeilRenderType.glintTransparencyShard()),
        CRUMBLING(VeilRenderType.crumblingTransparencyShard()),
        TRANSLUCENT(VeilRenderType.translucentTransparencyShard());

        private final RenderStateShard.TransparencyStateShard shard;

        BlendState(RenderStateShard.TransparencyStateShard shard) {
            this.shard = shard;
        }

        public RenderStateShard.TransparencyStateShard getShard() {
            return this.shard;
        }
    }
}
