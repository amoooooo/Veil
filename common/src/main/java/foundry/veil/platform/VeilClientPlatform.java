package foundry.veil.platform;

import foundry.veil.api.event.VeilAddShaderPreProcessorsEvent;
import foundry.veil.api.event.VeilPostProcessingEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Manages client platform-specific features.
 */
@ApiStatus.Internal
public interface VeilClientPlatform extends VeilPostProcessingEvent.Pre, VeilPostProcessingEvent.Post, VeilAddShaderPreProcessorsEvent {
}
