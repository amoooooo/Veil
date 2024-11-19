package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.resources.ResourceLocation;

public record VeilShaderLayer(ResourceLocation shaderId) implements RenderTypeLayer {

    public static final MapCodec<VeilShaderLayer> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.comapFlatMap(name -> VeilRenderSystem.renderer().getShaderManager().getShader(name) != null ? DataResult.success(name) : DataResult.error(() -> "Unknown Veil Shader: " + name), s -> s)
                    .fieldOf("name")
                    .forGetter(VeilShaderLayer::shaderId)
    ).apply(instance, VeilShaderLayer::new));

    @Override
    public void addLayer(VeilRenderTypeBuilder builder) {
        builder.shaderState(VeilRenderBridge.shaderState(this.shaderId));
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.VEIL_SHADER.get();
    }
}
