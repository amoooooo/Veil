package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;

public record PatchesLayer(int patchVertices) implements RenderTypeLayer {

    public static final MapCodec<PatchesLayer> CODEC = Codec.intRange(1, Integer.MAX_VALUE)
            .fieldOf("patchVertices")
            .xmap(PatchesLayer::new, PatchesLayer::patchVertices);

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        builder.addLayer(VeilRenderBridge.patchState(this.patchVertices));
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.PATCHES.get();
    }
}
