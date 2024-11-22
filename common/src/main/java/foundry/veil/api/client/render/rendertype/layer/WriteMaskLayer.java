package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.renderer.RenderStateShard;

public record WriteMaskLayer(boolean color, boolean depth) implements RenderTypeLayer {

    public static final MapCodec<WriteMaskLayer> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("color", true).forGetter(WriteMaskLayer::color),
            Codec.BOOL.optionalFieldOf("depth", true).forGetter(WriteMaskLayer::depth)
    ).apply(instance, WriteMaskLayer::new));

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        RenderStateShard.WriteMaskStateShard shard;
        if (this.color && this.depth) {
            shard = VeilRenderType.colorDepthWriteShard();
        } else if (this.color) {
            shard = VeilRenderType.colorWriteShard();
        } else if (this.depth) {
            shard = VeilRenderType.depthWriteShard();
        } else {
            shard = VeilRenderType.NO_WRITE;
        }
        builder.writeMaskState(shard);
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.WRITE_MASK.get();
    }
}
