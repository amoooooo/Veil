package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;

public record TextureLayer(ResourceLocation texture, boolean blur, boolean mipmap) implements RenderTypeLayer {

    public static final MapCodec<TextureLayer> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(TextureLayer::texture),
            Codec.BOOL.optionalFieldOf("blur", false).forGetter(TextureLayer::blur),
            Codec.BOOL.optionalFieldOf("mipmap", false).forGetter(TextureLayer::mipmap)
    ).apply(instance, TextureLayer::new));

    @Override
    public void addLayer(VeilRenderTypeBuilder builder) {
        builder.textureState(new RenderStateShard.TextureStateShard(this.texture, this.blur, this.blur));
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.TEXTURE.get();
    }
}
