package foundry.veil.platform;

import org.jetbrains.annotations.ApiStatus;

/**
 * Manages common platform-specific features.
 */
@ApiStatus.Internal
public interface VeilPlatform {

    /**
     * @return The detected platform operand
     */
    PlatformType getPlatformType();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * @return Whether Sodium is present
     */
    boolean isSodiumLoaded();

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    enum PlatformType {
        FORGE("Forge"),
        FABRIC("Fabric");

        private final String platformName;

        PlatformType(String platformName) {
            this.platformName = platformName;
        }

        /**
         * @return The name of the current platform.
         */
        public String getPlatformName() {
            return platformName;
        }
    }
}
