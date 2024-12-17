package foundry.veil.forge.mixin.compat.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import foundry.veil.ext.sodium.ChunkVertexEncoderVertexExtension;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderer.class)
public class BlockRendererMixin {

    @Shadow
    @Final
    private ChunkVertexEncoder.Vertex[] vertices;

    @Inject(method = "bufferQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;lightmap(I)I"), remap = false)
    public void bufferNormal(MutableQuadViewImpl quad, float[] brightnesses, Material material, CallbackInfo ci, @Local(ordinal = 0) int dstIndex, @Local(ordinal = 1) int srcIndex) {
        ChunkVertexEncoder.Vertex out = this.vertices[dstIndex];
        int packedNormal = quad.packedNormal(srcIndex);
        ((ChunkVertexEncoderVertexExtension) out).veil$setNormal(packedNormal);
    }
}
