package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.renderer.RenderStateShard;

public record DepthTestLayer(LayerTemplateValue<DepthState> mode) implements RenderTypeLayer {

    public static final MapCodec<DepthTestLayer> CODEC = LayerTemplateValue.enumCodec(DepthState.class)
            .fieldOf("mode")
            .xmap(DepthTestLayer::new, DepthTestLayer::mode);

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        builder.depthTestState(this.mode.parse(params).shard);
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.DEPTH_TEST.get();
    }

    public enum DepthState {
        NEVER(VeilRenderType.NEVER_DEPTH_TEST),
        LESS(VeilRenderType.LESS_DEPTH_TEST),
        EQUAL(VeilRenderType.equalDepthTestShard()),
        LEQUAL(VeilRenderType.lequalDepthTestShard()),
        GREATER(VeilRenderType.greaterDepthTestShard()),
        NOTEQUAL(VeilRenderType.NOTEQUAL_DEPTH_TEST),
        GEQUAL(VeilRenderType.GEQUAL_DEPTH_TEST),
        ALWAYS(VeilRenderType.noDepthTestShard());

        private final RenderStateShard.DepthTestStateShard shard;

        DepthState(RenderStateShard.DepthTestStateShard shard) {
            this.shard = shard;
        }

        public RenderStateShard.DepthTestStateShard getShard() {
            return this.shard;
        }
    }
}
