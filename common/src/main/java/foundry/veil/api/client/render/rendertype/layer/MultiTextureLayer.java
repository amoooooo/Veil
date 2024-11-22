package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.renderer.RenderStateShard;

import java.util.Arrays;

public record MultiTextureLayer(TextureLayer[] textures) implements RenderTypeLayer {

    public static final MapCodec<MultiTextureLayer> CODEC = TextureLayer.CODEC.codec()
            .listOf(2, Integer.MAX_VALUE)
            .fieldOf("textures")
            .xmap(textures -> new MultiTextureLayer(textures.toArray(TextureLayer[]::new)),
                    layer -> Arrays.asList(layer.textures));

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        RenderStateShard.MultiTextureStateShard.Builder textureBuilder = RenderStateShard.MultiTextureStateShard.builder();
        for (TextureLayer texture : this.textures) {
            textureBuilder.add(texture.texture().parse(params), texture.blur(), texture.mipmap());
        }
        builder.textureState(textureBuilder.build());
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.MULTI_TEXTURE.get();
    }
}
