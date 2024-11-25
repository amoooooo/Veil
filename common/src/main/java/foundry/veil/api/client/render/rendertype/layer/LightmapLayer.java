package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;

public record LightmapLayer(boolean enabled) implements RenderTypeLayer {

    public static final MapCodec<LightmapLayer> CODEC = Codec.BOOL
            .optionalFieldOf("enabled", true)
            .xmap(LightmapLayer::new, LightmapLayer::enabled);

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        builder.lightmapState(this.enabled ? VeilRenderType.lightmap() : VeilRenderType.noLightmap());
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.LIGHTMAP.get();
    }
}
