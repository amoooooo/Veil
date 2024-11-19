package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.renderer.RenderStateShard;

import java.util.Arrays;

public record MultiTextureLayer(TextureLayer[] textures) implements RenderTypeLayer {

    public static final MapCodec<MultiTextureLayer> CODEC = TextureLayer.CODEC.codec().listOf(2, Integer.MAX_VALUE)
            .xmap(textures -> new MultiTextureLayer(textures.toArray(TextureLayer[]::new)),
                    layer -> Arrays.asList(layer.textures)).fieldOf("textures");

    @Override
    public void addLayer(VeilRenderTypeBuilder builder) {
        RenderStateShard.MultiTextureStateShard.Builder textureBuilder = RenderStateShard.MultiTextureStateShard.builder();
        for (TextureLayer texture : this.textures) {
            textureBuilder.add(texture.texture(), texture.blur(), texture.mipmap());
        }
        builder.textureState(textureBuilder.build());
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.MULTI_TEXTURE.get();
    }
}
