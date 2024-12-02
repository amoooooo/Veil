package foundry.veil.impl.client.editor;

import com.google.common.base.Stopwatch;
import foundry.veil.Veil;
import foundry.veil.api.client.editor.SingleWindowEditor;
import foundry.veil.api.client.imgui.CodeEditor;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.opencl.*;
import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.type.ImInt;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_MEM_WRITE_ONLY;

@ApiStatus.Internal
public class OpenCLEditor extends SingleWindowEditor {

    public static final Component TITLE = Component.translatable("editor.veil.example.opencl.title");

    private static final Component RUN = Component.translatable("editor.veil.example.opencl.button.run");
    private static final Component EDIT = Component.translatable("editor.veil.example.opencl.button.edit");
    private static final Component NO_PLATFORM = Component.translatable("editor.veil.example.opencl.no_platform");
    private static final Component DEVICE = Component.translatable("editor.veil.example.opencl.device");
    private static final Component NO_DEVICE = Component.translatable("editor.veil.example.opencl.device.none");
    private static final Component ELEMENTS = Component.translatable("editor.veil.example.opencl.elements");
    private static final Component LOCAL_WORK_GROUPS = Component.translatable("editor.veil.example.opencl.local_work_groups");

    private final CodeEditor codeEditor;
    private String source;

    private CLEnvironment environment;
    private CLKernel kernel;

    private CLBuffer bufferA;
    private CLBuffer bufferB;
    private CLBuffer bufferC;
    private CLBuffer bufferD;

    private final ImInt elements = new ImInt(1_000_000);
    private final ImInt workGroups = new ImInt(1);

    public OpenCLEditor() {
        this.codeEditor = new CodeEditor(TITLE, Component.translatable("gui.veil.save"));
        this.codeEditor.setSaveCallback((source, errorConsumer) -> this.compileProgram(source));
        this.source = """
                void kernel example(global const int* A, global const int* B, global const int* C, global int* D) {
                    int i = get_global_id(0);
                    D[i] = A[i] + B[i] + C[i];
                }
                """;

        this.environment = null;
    }

    private void freeBuffers() {
        if (this.bufferA != null) {
            this.bufferA.free();
            this.bufferA = null;
        }
        if (this.bufferB != null) {
            this.bufferB.free();
            this.bufferB = null;
        }
        if (this.bufferC != null) {
            this.bufferC.free();
            this.bufferC = null;
        }
        if (this.bufferD != null) {
            this.bufferD.free();
            this.bufferD = null;
        }
    }

    private void initBuffers(int itemCount) throws CLException {
        if (this.bufferA == null) {
            this.bufferA = this.kernel.createBuffer(CL_MEM_READ_ONLY, (long) itemCount * Integer.BYTES);
        }
        if (this.bufferB == null) {
            this.bufferB = this.kernel.createBuffer(CL_MEM_READ_ONLY, (long) itemCount * Integer.BYTES);
        }
        if (this.bufferC == null) {
            this.bufferC = this.kernel.createBuffer(CL_MEM_READ_ONLY, (long) itemCount * Integer.BYTES);
        }
        if (this.bufferD == null) {
            this.bufferD = this.kernel.createBuffer(CL_MEM_WRITE_ONLY, (long) itemCount * Integer.BYTES);
        }
    }

    private void selectDevice(VeilOpenCL.DeviceInfo deviceInfo) {
        if (this.environment != null && Objects.equals(this.environment.getDevice(), deviceInfo)) {
            return;
        }

        if (!deviceInfo.compilerAvailable()) {
            return;
        }

        CLEnvironment environment = VeilOpenCL.get().getEnvironment();
        if (environment == null) {
            Veil.LOGGER.error("Failed to change CL device");
            return;
        }

        this.free();
        this.environment = environment;

        if (!this.source.isBlank()) {
            this.compileProgram(this.source);
        }
    }

    private void compileProgram(String source) {
        if (this.environment == null) {
            return;
        }

        ResourceLocation name = Veil.veilPath("test");
        this.environment.loadProgram(name, source);

        try {
            this.kernel = this.environment.createKernel(name, "example");
        } catch (CLException e) {
            Veil.LOGGER.error("Failed to compile program");
            this.kernel = null;
            return;
        }

        this.source = source;
    }

    @Override
    protected void renderComponents() {
        VeilOpenCL.PlatformInfo[] platforms = VeilOpenCL.get().getPlatforms();
        if (platforms.length == 0) {
            VeilImGuiUtil.component(NO_PLATFORM);
            return;
        }

        VeilOpenCL.DeviceInfo deviceInfo = this.environment != null ? this.environment.getDevice() : null;
        if (ImGui.beginCombo(DEVICE.getString(), deviceInfo == null ? NO_DEVICE.getString() : deviceInfo.name())) {
            for (VeilOpenCL.PlatformInfo platform : platforms) {
                for (VeilOpenCL.DeviceInfo device : platform.devices()) {
                    if (device.compilerAvailable() && ImGui.selectable(platform.vendor() + " " + device.name(), deviceInfo == device)) {
                        this.selectDevice(device);
                    }
                }
            }
            ImGui.endCombo();
        }

        ImGui.beginDisabled(deviceInfo == null);
        if (ImGui.button(EDIT.getString())) {
            this.codeEditor.show(null, this.source);
        }
        ImGui.endDisabled();

        ImGui.beginDisabled(this.kernel == null);
        ImGui.sameLine();
        if (ImGui.button(RUN.getString())) {
            Stopwatch upload = Stopwatch.createStarted();
            int itemCount = this.elements.get();
            IntBuffer A = MemoryUtil.memAllocInt(itemCount);
            IntBuffer B = MemoryUtil.memAllocInt(itemCount);
            IntBuffer C = MemoryUtil.memAllocInt(itemCount);
            IntBuffer D = MemoryUtil.memAllocInt(itemCount);
            try {
                for (int i = 0; i < itemCount; i++) {
                    A.put(i, i);
                    B.put(i, itemCount - i);
                    C.put(i, 2 * i);
                }
                upload.stop();

                Stopwatch execute = Stopwatch.createStarted();
                this.initBuffers(itemCount);

                this.bufferA.writeAsync(0, A, null);
                this.bufferB.writeAsync(0, B, null);
                this.bufferC.writeAsync(0, C, null);

                this.kernel.setPointers(0, this.bufferA);
                this.kernel.setPointers(1, this.bufferB);
                this.kernel.setPointers(2, this.bufferC);
                this.kernel.setPointers(3, this.bufferD);

                this.kernel.execute(itemCount, this.workGroups.get());

                this.bufferD.readAsync(0, D, null);

                this.environment.finish();

                execute.stop();
                System.out.printf("Done in " + (upload.elapsed(TimeUnit.NANOSECONDS) + execute.elapsed(TimeUnit.NANOSECONDS)) / 1_000_000 + "ms (%s upload, %s execute, %sns/item)\n", upload, execute, execute.elapsed(TimeUnit.NANOSECONDS) / itemCount);
            } catch (Throwable t) {
                Veil.LOGGER.error("Failed to run OpenCL", t);
            } finally {
                MemoryUtil.memFree(A);
                MemoryUtil.memFree(B);
                MemoryUtil.memFree(C);
                MemoryUtil.memFree(D);
            }
        }
        ImGui.endDisabled();

        if (ImGui.dragScalar(ELEMENTS.getString(), ImGuiDataType.U32, this.elements, 1_000, 0, 100_000_000)) {
            this.freeBuffers();
        }

        int max = this.kernel != null ? this.kernel.getMaxWorkGroupSize() : Integer.MAX_VALUE;
        ImGui.beginDisabled(this.kernel == null);
        ImGui.sliderScalar(LOCAL_WORK_GROUPS.getString(), ImGuiDataType.U32, this.workGroups, 1, max);
        ImGui.endDisabled();
    }

    @Override
    public void onHide() {
        super.onHide();
        this.free();
    }

    @Override
    public void free() {
        this.freeBuffers();
    }

    @Override
    public void render() {
        super.render();
        this.codeEditor.renderWindow();
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public Component getGroup() {
        return EXAMPLE_GROUP;
    }
}
