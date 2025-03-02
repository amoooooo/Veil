package foundry.veil.impl.client.imgui;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.ObjIntConsumer;

/**
 * Manages the internal ImGui state.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public interface VeilImGui {

    void start();

    void stop();

    void beginFrame();

    void endFrame();

    void onGrabMouse();

    void toggle();

    void updateFonts();

    void addImguiShaders(ObjIntConsumer<ResourceLocation> registry);

    boolean mouseButtonCallback(long window, int button, int action, int mods);

    boolean scrollCallback(long window, double xOffset, double yOffset);

    boolean keyCallback(long window, int key, int scancode, int action, int mods);

    boolean charCallback(long window, int codepoint);

    boolean shouldHideMouse();
}
