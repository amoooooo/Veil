package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.renderer.RenderStateShard;

public record CullLayer(LayerTemplateValue<CullFace> face) implements RenderTypeLayer {

    public static final MapCodec<CullLayer> CODEC = LayerTemplateValue.enumCodec(CullFace.class)
            .optionalFieldOf("face", LayerTemplateValue.raw(CullFace.BACK))
            .xmap(CullLayer::new, CullLayer::face);

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        CullFace face = this.face.parse(params);
        if (face == CullFace.NONE) {
            builder.cullState(VeilRenderType.noCullShard());
        } else {
            builder.cullState(VeilRenderType.cullShard());
            builder.addLayer(face.shard);
        }
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.CULL.get();
    }

    public enum CullFace {
        FRONT(VeilRenderType.CULL_FRONT),
        BACK(VeilRenderType.CULL_BACK),
        FRONT_AND_BACK(VeilRenderType.CULL_FRONT_AND_BACK),
        NONE(null);

        private final RenderStateShard shard;

        CullFace(RenderStateShard shard) {
            this.shard = shard;
        }

        public RenderStateShard getShard() {
            return this.shard;
        }
    }
}
