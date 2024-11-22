package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;

public record OverlayLayer(boolean enabled) implements RenderTypeLayer {

    public static final MapCodec<OverlayLayer> CODEC = Codec.BOOL
            .optionalFieldOf("enabled", true)
            .xmap(OverlayLayer::new, OverlayLayer::enabled);

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        builder.overlayState(this.enabled ? VeilRenderType.overlay() : VeilRenderType.noOverlay());
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.OVERLAY.get();
    }
}
