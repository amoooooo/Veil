package foundry.veil.ext.sodium;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ChunkShaderOptionsExtension {

    int veil$getActiveBuffers();

    void veil$setActiveBuffers(int activeBuffers);
}
