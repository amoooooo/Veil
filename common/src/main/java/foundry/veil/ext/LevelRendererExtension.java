package foundry.veil.ext;

import foundry.veil.api.client.render.CullFrustum;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface LevelRendererExtension extends LevelRendererBlockLayerExtension {

    void veil$markChunksDirty();

    CullFrustum veil$getCullFrustum();
}
