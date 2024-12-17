package foundry.veil.forge.mixin.compat.sodium;

import foundry.veil.ext.sodium.ChunkVertexEncoderVertexExtension;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkVertexEncoder.Vertex.class)
public class ChunkVertexEncoderVertexMixin implements ChunkVertexEncoderVertexExtension {

    @Unique
    private int veil$packedNormal;

    @Override
    public int veil$getPackedNormal() {
        return this.veil$packedNormal;
    }

    @Override
    public void veil$setNormal(int packedNormal) {
        this.veil$packedNormal = packedNormal;
    }
}
