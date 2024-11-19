package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;

public record VanillaShaderLayer(String shaderName) implements RenderTypeLayer {

    public static final MapCodec<VanillaShaderLayer> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.comapFlatMap(name -> Minecraft.getInstance().gameRenderer.getShader(name) != null ? DataResult.success(name) : DataResult.error(() -> "Unknown Vanilla Shader: " + name), s -> s)
                    .fieldOf("name")
                    .forGetter(VanillaShaderLayer::shaderName)
    ).apply(instance, VanillaShaderLayer::new));

    @Override
    public void addLayer(VeilRenderTypeBuilder builder) {
        builder.shaderState(new RenderStateShard.ShaderStateShard(() -> Minecraft.getInstance().gameRenderer.getShader(this.shaderName)));
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.VANILLA_SHADER.get();
    }
}
