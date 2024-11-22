package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.renderer.RenderStateShard;

import java.util.Optional;
import java.util.OptionalDouble;

public record LineLayer(OptionalDouble width) implements RenderTypeLayer {

    public static final MapCodec<LineLayer> CODEC = Codec.DOUBLE
            .optionalFieldOf("width")
            .xmap(width -> new LineLayer(width.map(OptionalDouble::of).orElseGet(OptionalDouble::empty)),
                    layer -> layer.width().isPresent() ? Optional.of(layer.width().getAsDouble()) : Optional.empty());

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        builder.lineState(new RenderStateShard.LineStateShard(this.width));
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.LINE.get();
    }
}
