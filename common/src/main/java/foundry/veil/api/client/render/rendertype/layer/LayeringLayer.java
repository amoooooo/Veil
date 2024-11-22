package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.renderer.RenderStateShard;

public record LayeringLayer(LayerTemplateValue<LayeringState> mode) implements RenderTypeLayer {

    public static final MapCodec<LayeringLayer> CODEC = LayerTemplateValue.enumCodec(LayeringState.class)
            .optionalFieldOf("mode", LayerTemplateValue.raw(LayeringState.POLYGON_OFFSET))
            .xmap(LayeringLayer::new, LayeringLayer::mode);

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        builder.layeringState(this.mode.parse(params).shard);
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.LAYERING.get();
    }

    public enum LayeringState {
        NONE(VeilRenderType.noLayering()),
        POLYGON_OFFSET(VeilRenderType.polygonOffsetLayering()),
        VIEW_OFFSET(VeilRenderType.viewOffsetLayering());

        private final RenderStateShard.LayeringStateShard shard;

        LayeringState(RenderStateShard.LayeringStateShard shard) {
            this.shard = shard;
        }

        public RenderStateShard.LayeringStateShard getShard() {
            return this.shard;
        }
    }
}
