package foundry.veil.impl.client.imgui;

import foundry.veil.Veil;
import foundry.veil.api.client.imgui.VeilImGui;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.imgui.style.VeilImGuiStylesheet;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.ImPlotContext;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.internal.ImGuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.NativeResource;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ObjIntConsumer;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

/**
 * Manages the internal ImGui state.
 */
@ApiStatus.Internal
public class VeilImGuiImpl implements VeilImGui, NativeResource {

    private static VeilImGui instance = new InactiveVeilImGuiImpl();

    private final ImGuiImplGlfw implGlfw;
    private final ImGuiImplGl3 implGl3;
    private final ThreadLocal<ImguiState> state;
    private final ImGuiContext imGuiContext;
    private final ImPlotContext imPlotContext;
    private final AtomicBoolean active;

    private VeilImGuiImpl(long window) {
        this.implGlfw = new VeilImGuiImplGlfw(this);
        this.implGl3 = new ImGuiImplGl3();

        this.state = ThreadLocal.withInitial(ImguiState::new);
        ImGuiContext oldImGuiContext = new ImGuiContext(ImGui.getCurrentContext().ptr);
        ImPlotContext oldImPlotContext = new ImPlotContext(ImPlot.getCurrentContext().ptr);

        this.imGuiContext = new ImGuiContext(ImGui.createContext().ptr);
        this.imPlotContext = new ImPlotContext(ImPlot.createContext().ptr);
        this.active = new AtomicBoolean();
        this.implGlfw.init(window, true);
        this.implGl3.init("#version 410 core");

        VeilImGuiStylesheet.initStyles();

        ImGuiIO io = ImGui.getIO();
        io.setGetClipboardTextFn(new ImStrSupplier() {
            @Override
            public String get() {
                return Minecraft.getInstance().keyboardHandler.getClipboard();
            }
        });
        io.setSetClipboardTextFn(new ImStrConsumer() {
            @Override
            public void accept(String str) {
                Minecraft.getInstance().keyboardHandler.setClipboard(str);
            }
        });

        ImGui.setCurrentContext(oldImGuiContext);
        ImPlot.setCurrentContext(oldImPlotContext);
    }

    @Override
    public void start() {
        this.state.get().start(this.imGuiContext, this.imPlotContext);
    }

    @Override
    public void stop() {
        this.state.get().end();
    }

    @Override
    public void beginFrame() {
        this.start();

        if (this.active.get()) {
            Veil.LOGGER.error("ImGui failed to render previous frame, disposing");
            ImGui.endFrame();
        }
        this.active.set(true);
        this.implGlfw.newFrame();
        ImGui.newFrame();

        AdvancedFboImGuiAreaImpl.begin();
        VeilRenderSystem.renderer().getEditorManager().render();

        this.stop();
    }

    @Override
    public void endFrame() {
        AdvancedFboImGuiAreaImpl.end();

        if (!this.active.get()) {
            Veil.LOGGER.error("ImGui state de-synced");
            this.stop();
            return;
        }
        this.start();

        this.active.set(false);
        VeilRenderSystem.renderer().getEditorManager().renderLast();
        ImGui.render();
        this.implGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backupWindowPtr);
        }

        this.state.get().forceEnd();
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
        try {
            Field field = ImGuiImplGl3.class.getDeclaredField("gShaderHandle");
            field.setAccessible(true);
            int handle = field.getInt(this.implGl3);
            registry.accept(ResourceLocation.fromNamespaceAndPath("imgui", "blit"), handle);
        } catch (Exception e) {
            Veil.LOGGER.warn("Failed to add ImGui shader", e);
        }
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
        this.start();
        this.implGlfw.dispose();
        this.implGl3.dispose();
        ImGui.destroyContext(this.imGuiContext);
        ImPlot.destroyContext(this.imPlotContext);
        this.stop();
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

    private static class ImguiState {

        private final ImGuiContext oldImGuiContext;
        private final ImPlotContext oldImPlotContext;
        private int beginLayer;

        public ImguiState() {
            this.oldImGuiContext = new ImGuiContext(0L);
            this.oldImPlotContext = new ImPlotContext(0L);
        }

        public void start(ImGuiContext imGuiContext, ImPlotContext imPlotContext) {
            this.beginLayer++;

            if (ImGui.getCurrentContext().ptr == imGuiContext.ptr) {
                return;
            }

            this.oldImGuiContext.ptr = ImGui.getCurrentContext().ptr;
            this.oldImPlotContext.ptr = ImPlot.getCurrentContext().ptr;

            ImGui.setCurrentContext(imGuiContext);
            ImPlot.setCurrentContext(imPlotContext);
        }

        public void end() {
            if (--this.beginLayer == 0) {
                ImGui.setCurrentContext(this.oldImGuiContext);
                ImPlot.setCurrentContext(this.oldImPlotContext);
                this.oldImGuiContext.ptr = 0L;
                this.oldImPlotContext.ptr = 0L;
            }
        }

        public void forceEnd() {
            if (this.beginLayer > 1) {
                Veil.LOGGER.error("Mismatched begin/end during frame");
                this.beginLayer = 1;
            }
            this.end();
        }
    }
}
