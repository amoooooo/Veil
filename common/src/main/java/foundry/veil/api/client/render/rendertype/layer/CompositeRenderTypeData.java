package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import foundry.veil.api.client.util.VertexFormatCodec;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public record CompositeRenderTypeData(VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                      boolean affectsCrumbling, boolean sort, boolean outline,
                                      List<RenderTypeLayer[]> layers) {

    private static final Codec<List<List<RenderTypeLayer>>> LAYER_CODEC = Codec.withAlternative(
            RenderTypeLayer.CODEC.listOf().listOf().flatXmap(
                    list -> list.size() < 2 ? DataResult.error(() -> "Expected at least 2 layers") : DataResult.success(list),
                    list -> list.size() < 2 ? DataResult.error(() -> "Expected at least 2 layers") : DataResult.success(list)),
            RenderTypeLayer.CODEC.listOf().flatXmap(
                    layers -> DataResult.success(Collections.singletonList(layers)),
                    list -> list.size() == 1 ? DataResult.success(list.getFirst()) : DataResult.error(() -> "Expected 1 shard")
            )).flatXmap(
            list -> list.isEmpty() ? DataResult.error(() -> "Expected at least 1 layer") : DataResult.success(list),
            list -> list.isEmpty() ? DataResult.error(() -> "Expected at least 1 layer") : DataResult.success(list));

    public static final Codec<CompositeRenderTypeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VertexFormatCodec.CODEC.fieldOf("format").forGetter(CompositeRenderTypeData::format),
            VertexFormatCodec.MODE_CODEC.fieldOf("mode").forGetter(CompositeRenderTypeData::mode),
            VertexFormatCodec.BUFFER_SIZE_CODEC.fieldOf("bufferSize").forGetter(CompositeRenderTypeData::bufferSize),
            Codec.BOOL.optionalFieldOf("affectsCrumbling", false).forGetter(CompositeRenderTypeData::sort),
            Codec.BOOL.optionalFieldOf("sort", false).forGetter(CompositeRenderTypeData::sort),
            Codec.BOOL.optionalFieldOf("outline", false).forGetter(CompositeRenderTypeData::outline),
            LAYER_CODEC.fieldOf("layers").forGetter(state -> state.layers().stream().map(Arrays::asList).toList())
    ).apply(instance, (format, mode, bufferSize, affectsCrumbling, sort, outline, layersList) -> {
        List<RenderTypeLayer[]> layers = new ArrayList<>(layersList.size());
        for (List<RenderTypeLayer> shards : layersList) {
            layers.add(shards.toArray(RenderTypeLayer[]::new));
        }
        return new CompositeRenderTypeData(format, mode, bufferSize, affectsCrumbling, sort, outline, Collections.unmodifiableList(layers));
    }));

    @ApiStatus.Internal
    public RenderType createRenderType(String name, Object... params) {
        RenderType[] renderTypes = new RenderType[this.layers.size()];
        for (int i = 0; i < this.layers.size(); i++) {
            RenderTypeLayer[] shards = this.layers.get(i);

            VeilRenderTypeBuilder builder = VeilRenderBridge.create(RenderType.CompositeState.builder());
            for (RenderTypeLayer shard : shards) {
                shard.addShard(builder, params);
            }

            RenderType.CompositeState state = builder.create(this.outline);
            renderTypes[i] = RenderType.create(name, this.format, this.mode, this.bufferSize, this.affectsCrumbling, this.sort, state);
        }
        return VeilRenderType.layered(renderTypes);
    }
}
