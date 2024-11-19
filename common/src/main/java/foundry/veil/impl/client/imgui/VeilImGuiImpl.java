package foundry.veil.impl.client.imgui;

import foundry.veil.Veil;
import foundry.veil.api.client.imgui.VeilImGui;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.imgui.style.VeilImGuiStylesheet;
import foundry.veil.mixin.client.imgui.ImGuiImplGl3Mixin;
import imgui.ImGui;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.ImPlotContext;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.internal.ImGuiContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.ObjIntConsumer;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

/**
 * Manages the internal ImGui state.
 */
@ApiStatus.Internal
public class VeilImGuiImpl implements VeilImGui {

    private static VeilImGui instance;

    private final ImGuiImplGlfw implGlfw;
    private final ImGuiImplGl3 implGl3;
    private final ImGuiContext oldImGuiContext;
    private final ImPlotContext oldImPlotContext;
    private final ImGuiContext imGuiContext;
    private final ImPlotContext imPlotContext;
    private boolean active;
    private int beginLayer;

    private VeilImGuiImpl(long window) {
        this.implGlfw = new VeilImGuiImplGlfw();
        this.implGl3 = new ImGuiImplGl3();

        this.oldImGuiContext = new ImGuiContext(ImGui.getCurrentContext().ptr);
        this.oldImPlotContext = new ImPlotContext(ImPlot.getCurrentContext().ptr);

        this.imGuiContext = new ImGuiContext(ImGui.createContext().ptr);
        this.imPlotContext = new ImPlotContext(ImPlot.createContext().ptr);
        this.implGlfw.init(window, true);
        this.implGl3.init("#version 410 core");

        VeilImGuiStylesheet.initStyles();

        ImGui.setCurrentContext(this.oldImGuiContext);
        ImPlot.setCurrentContext(this.oldImPlotContext);
    }

    @Override
    public void begin() {
        this.beginLayer++;

        if (ImGui.getCurrentContext().ptr == this.imGuiContext.ptr) {
            return;
        }

        this.oldImGuiContext.ptr = ImGui.getCurrentContext().ptr;
        this.oldImPlotContext.ptr = ImPlot.getCurrentContext().ptr;

        ImGui.setCurrentContext(this.imGuiContext);
        ImPlot.setCurrentContext(this.imPlotContext);
    }

    @Override
    public void beginFrame() {
        this.begin();

        if (this.active) {
            Veil.LOGGER.error("ImGui failed to render previous frame, disposing");
            ImGui.endFrame();
        }
        this.active = true;
        this.implGlfw.newFrame();
        ImGui.newFrame();

        VeilRenderSystem.renderer().getEditorManager().render();

        this.end();
    }

    @Override
    public void endFrame() {
        if (!this.active) {
            Veil.LOGGER.error("ImGui state de-synced");
            this.end();
            return;
        }
        this.begin();

        this.active = false;
        VeilRenderSystem.renderer().getEditorManager().renderLast();
        ImGui.render();
        this.implGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backupWindowPtr);
        }

        if (this.beginLayer > 1) {
            Veil.LOGGER.error("Mismatched begin/end during frame");
            this.beginLayer = 1;
        }
        this.end();
    }

    @Override
    public void end() {
        if (--this.beginLayer == 0) {
            ImGui.setCurrentContext(this.oldImGuiContext);
            ImPlot.setCurrentContext(this.oldImPlotContext);
        }
    }

    @Override
    public void onGrabMouse() {
        ImGui.setWindowFocus(null);
    }

    @Override
    public void toggle() {
        VeilRenderSystem.renderer().getEditorManager().toggle();
    }

    @Override
    public void updateFonts() {
        this.implGl3.updateFontsTexture();
    }

    @Override
    public void addImguiShaders(ObjIntConsumer<ResourceLocation> registry) {
        registry.accept(ResourceLocation.fromNamespaceAndPath("imgui", "blit"), ((ImGuiImplGl3Mixin) (Object) this.implGl3).getGShaderHandle());
    }

    @Override
    public boolean mouseButtonCallback(long window, int button, int action, int mods) {
        return ImGui.getIO().getWantCaptureMouse();
    }

    @Override
    public boolean scrollCallback(long window, double xOffset, double yOffset) {
        return ImGui.getIO().getWantCaptureMouse();
    }

    @Override
    public boolean keyCallback(long window, int key, int scancode, int action, int mods) {
        return ImGui.getIO().getWantCaptureKeyboard();
    }

    @Override
    public boolean charCallback(long window, int codepoint) {
        return ImGui.getIO().getWantCaptureKeyboard();
    }

    @Override
    public boolean shouldHideMouse() {
        return ImGui.getIO().getWantCaptureMouse();
    }

    @Override
    public void free() {
        this.begin();
        this.implGlfw.dispose();
        this.implGl3.dispose();
        ImGui.destroyContext(this.imGuiContext);
        ImPlot.destroyContext(this.imPlotContext);
        this.end();
    }

    public static void init(long window) {
        try {
            instance = Veil.IMGUI ? new VeilImGuiImpl(window) : new InactiveVeilImGuiImpl();
        } catch (Throwable t) {
            Veil.LOGGER.error("Failed to load ImGui", t);
            instance = new InactiveVeilImGuiImpl();
        }
    }

    public static void setImGuiPath() {
        if (System.getProperty("os.arch").equals("arm") || System.getProperty("os.arch").startsWith("aarch64")) {
            // ImGui infers a path for loading the library using this name property
            // Essential that this property is set, before any ImGui-adjacent native code is loaded
            System.setProperty("imgui.library.name", "libimgui-javaarm64.dylib");
        }
    }

    public static VeilImGui get() {
        return instance;
    }
}
