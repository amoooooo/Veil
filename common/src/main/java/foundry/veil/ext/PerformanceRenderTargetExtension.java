package foundry.veil.ext;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface PerformanceRenderTargetExtension {

    void veil$clearColorBuffer(boolean clearError);
}
