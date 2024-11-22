package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;

public record TextureLayer(LayerTemplateValue<ResourceLocation> texture,
                           boolean blur,
                           boolean mipmap) implements RenderTypeLayer {

    public static final MapCodec<TextureLayer> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LayerTemplateValue.LOCATION_CODEC.fieldOf("texture").forGetter(TextureLayer::texture),
            Codec.BOOL.optionalFieldOf("blur", false).forGetter(TextureLayer::blur),
            Codec.BOOL.optionalFieldOf("mipmap", false).forGetter(TextureLayer::mipmap)
    ).apply(instance, TextureLayer::new));

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        ResourceLocation location = this.texture.parse(params);
        builder.textureState(new RenderStateShard.TextureStateShard(location, this.blur, this.blur));
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.TEXTURE.get();
    }
}
