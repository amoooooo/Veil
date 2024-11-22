package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.Codec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;

public interface RenderTypeLayer {

    Codec<RenderTypeLayer> CODEC = RenderTypeLayerRegistry.REGISTRY.byNameCodec().dispatch(RenderTypeLayer::getType, RenderTypeLayerRegistry.LayerType::codec);

    void addShard(VeilRenderTypeBuilder builder, Object... params);

    RenderTypeLayerRegistry.LayerType<?> getType();
}
