package foundry.veil.fabric.mixin.client.stage.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.fabric.ext.LevelRendererExtension;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SodiumWorldRenderer.class)
public class SodiumWorldRendererMixin {

    @Inject(method = "drawChunkLayer", at = @At("TAIL"), remap = false)
    public void postRenderChunkLayer(RenderType renderLayer, ChunkRenderMatrices matrices, double x, double y, double z, CallbackInfo ci) {
        ((LevelRendererExtension) Minecraft.getInstance().levelRenderer).veil$renderStage(renderLayer, matrices.modelView(), matrices.projection());
    }
}
