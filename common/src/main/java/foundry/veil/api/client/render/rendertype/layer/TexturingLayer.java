package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import foundry.veil.Veil;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import org.joml.Matrix4f;

import java.util.Locale;

public record TexturingLayer(float scale) implements RenderTypeLayer {

    public static final MapCodec<TexturingLayer> CODEC = Codec.FLOAT.fieldOf("scale")
            .xmap(TexturingLayer::new, TexturingLayer::scale);
    private static final Matrix4f MATRIX = new Matrix4f();

    @Override
    public void addShard(VeilRenderTypeBuilder builder, Object... params) {
        if (this.scale != 1) {
            builder.texturingState(new RenderStateShard.TexturingStateShard(Veil.MODID + ":glint_texturing", () -> {
                long time = (long) ((double) Util.getMillis() * Minecraft.getInstance().options.glintSpeed().get() * 8.0);
                float x = (float) (time % 110000L) / 110000.0F;
                float y = (float) (time % 30000L) / 30000.0F;
                MATRIX.setTranslation(-x, y, 0.0F);
                MATRIX.rotateZ((float) (Math.PI / 18.0)).scale(this.scale);
                RenderSystem.setTextureMatrix(MATRIX);
            }, RenderSystem::resetTextureMatrix));
        }
    }

    @Override
    public RenderTypeLayerRegistry.LayerType<?> getType() {
        return RenderTypeLayerRegistry.TEXTURING.get();
    }
}
