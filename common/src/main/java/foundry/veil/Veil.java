package foundry.veil;

import foundry.veil.api.client.imgui.VeilImGui;
import foundry.veil.api.molang.VeilMolang;
import foundry.veil.example.VeilExampleContent;
import foundry.veil.impl.client.imgui.VeilImGuiImpl;
import foundry.veil.platform.VeilPlatform;
import gg.moonflower.molangcompiler.api.MolangCompiler;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public class Veil {

    public static final String MODID = "veil";
    public static final Logger LOGGER = LoggerFactory.getLogger("Veil");
    public static final boolean DEBUG;
    public static final boolean IMGUI;
    public static final boolean VERBOSE_SHADER_ERRORS;

    private static final VeilPlatform PLATFORM = ServiceLoader.load(VeilPlatform.class).findFirst().orElseThrow(() -> new RuntimeException("Veil expected platform implementation"));

    public static final boolean SODIUM = PLATFORM.isSodiumLoaded();

    static {
        DEBUG = System.getProperty("veil.debug") != null;
        IMGUI = System.getProperty("veil.disableImgui") == null;
        VERBOSE_SHADER_ERRORS = System.getProperty("veil.verboseShaderErrors") != null;
    }

    @ApiStatus.Internal
    public static void init() {
        LOGGER.info("Veil is initializing.");
        if (DEBUG) {
            LOGGER.info("Veil Debug Enabled");
        }
        if (!IMGUI) {
            LOGGER.info("ImGui Disabled");
        }
        VeilMolang.set(MolangCompiler.create(MolangCompiler.DEFAULT_FLAGS, Veil.class.getClassLoader()));

        if (platform().isDevelopmentEnvironment())
            VeilExampleContent.init();
    }

    /**
     * <p>Enables writing ImGui to the screen. This useful for debugging during the normal render loop.</p>
     * <p>Be sure to call {@link #endImGui()} when done.</p>
     */
    public static VeilImGui beginImGui() {
        VeilImGui imGui = VeilImGuiImpl.get();
        imGui.begin();
        return imGui;
    }

    /**
     * Disables ImGui writing. This should be called after done using ImGui during the main render loop.
     */
    public static void endImGui() {
        VeilImGuiImpl.get().end();
    }

    public static ResourceLocation veilPath(String path) {
        return new ResourceLocation(MODID, path);
    }

    public static VeilPlatform platform() {
        return PLATFORM;
    }
}
