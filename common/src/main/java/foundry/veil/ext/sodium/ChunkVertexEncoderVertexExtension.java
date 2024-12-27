package foundry.veil.ext.sodium;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ChunkVertexEncoderVertexExtension {

    int veil$getPackedNormal();

    void veil$setNormal(int packedNormal);
}
