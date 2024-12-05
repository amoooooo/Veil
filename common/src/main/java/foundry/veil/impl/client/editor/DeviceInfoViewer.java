package foundry.veil.impl.client.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.editor.SingleWindowInspector;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilShaderLimits;
import foundry.veil.api.opencl.VeilOpenCL;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.util.*;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL30C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL42C.*;
import static org.lwjgl.opengl.GL43C.*;

@ApiStatus.Internal
public class DeviceInfoViewer extends SingleWindowInspector {

    public static final Component TITLE = Component.translatable("inspector.veil.device_info.title");

    private static final Component UNSUPPORTED = Component.translatable("inspector.veil.device_info.unsupported");
    private static final Component YES = CommonComponents.GUI_YES.copy().withStyle(style -> style.withColor(0xFF00FF00));
    private static final Component NO = CommonComponents.GUI_NO.copy().withStyle(style -> style.withColor(0xFF0000FF));

    private static final Component GL_FEATURE_FLAG = Component.translatable("inspector.veil.device_info.opengl.feature_flag");
    private static final Component GL_UNIFORM = Component.translatable("inspector.veil.device_info.opengl.uniform");
    private static final Component GL_TRANSFORM_FEEDBACK = Component.translatable("inspector.veil.device_info.opengl.transform_feedback");
    private static final Component GL_ATOMIC_COUNTER = Component.translatable("inspector.veil.device_info.opengl.atomic_counter");
    private static final Component GL_SHADER_STORAGE = Component.translatable("inspector.veil.device_info.opengl.shader_storage");
    private static final Component GL_TEXTURE = Component.translatable("inspector.veil.device_info.opengl.texture");
    private static final Component GL_FRAMEBUFFER = Component.translatable("inspector.veil.device_info.opengl.framebuffer");
    private static final Component CL_PLATFORMS = Component.translatable("inspector.veil.device_info.opencl.platforms");
    private static final Component CL_DEVICES = Component.translatable("inspector.veil.device_info.opencl.devices");

    private static final Component CL_DEVICE_DEFAULT = Component.translatable("inspector.veil.device_info.opencl.device.default");
    private static final Component CL_DEVICE_CPU = Component.translatable("inspector.veil.device_info.opencl.device.cpu");
    private static final Component CL_DEVICE_GPU = Component.translatable("inspector.veil.device_info.opencl.device.gpu");
    private static final Component CL_DEVICE_ACCELERATOR = Component.translatable("inspector.veil.device_info.opencl.device.accelerator");

    private static final Map<Integer, Component> SHADER_TYPES;
    private static final int TEXT_COLOR = 0xFFAAAAAA;

    static {
        Map<Integer, Component> map = new LinkedHashMap<>();
        map.put(GL_VERTEX_SHADER, Component.translatable("inspector.veil.shader.vertex_shader"));
        map.put(GL_TESS_CONTROL_SHADER, Component.translatable("inspector.veil.shader.tess_control_shader"));
        map.put(GL_TESS_EVALUATION_SHADER, Component.translatable("inspector.veil.shader.tess_eval_shader"));
        map.put(GL_GEOMETRY_SHADER, Component.translatable("inspector.veil.shader.geometry_shader"));
        map.put(GL_FRAGMENT_SHADER, Component.translatable("inspector.veil.shader.fragment_shader"));
        map.put(GL_COMPUTE_SHADER, Component.translatable("inspector.veil.shader.compute_shader"));
        SHADER_TYPES = Collections.unmodifiableMap(map);
    }

    private static void text(String key, @Nullable Object value, @Nullable String tooltip) {
        if (value != null) {
            MutableComponent valueComponent = value instanceof MutableComponent c ? c : Component.literal(value.toString());
            VeilImGuiUtil.component(Component.translatable(key, valueComponent.withStyle(style -> style.withColor(0xFFFFFFFF))));
        } else {
            int color = VeilImGuiUtil.getColor(ImGuiCol.TextDisabled);
            VeilImGuiUtil.component(Component.translatable(key, UNSUPPORTED).withStyle(style -> style.withColor(color)));
        }
        if (tooltip != null) {
            ImGui.sameLine();
            VeilImGuiUtil.tooltip(tooltip);
        }
    }

    private static void flagText(String key, boolean supported, @Nullable String tooltip) {
        VeilImGuiUtil.component(Component.translatable(key, supported ? YES : NO));
        if (tooltip != null) {
            ImGui.sameLine();
            VeilImGuiUtil.tooltip(tooltip);
        }
    }

    private static void title(Component component) {
        ImGui.pushStyleColor(ImGuiCol.Text, 0xFFFFFFFF);
        VeilImGuiUtil.component(component);
        ImGui.popStyleColor();
    }

    private void renderOpenGL() {
        ImGui.pushStyleColor(ImGuiCol.Text, 0xFFFFFFFF);
        text("inspector.veil.device_info.opengl.vendor", glGetString(GL_VENDOR), null);
        text("inspector.veil.device_info.opengl.renderer", glGetString(GL_RENDERER), null);
        text("inspector.veil.device_info.opengl.version", glGetString(GL_VERSION), null);
        ImGui.popStyleColor();
        ImGui.separator();

        title(GL_FEATURE_FLAG);
        flagText("inspector.veil.device_info.opengl.feature_flag.compute", VeilRenderSystem.computeSupported(), "Whether compute shaders can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.atomic_counter", VeilRenderSystem.atomicCounterSupported(), "Whether atomic counters can be used in shaders");
        flagText("inspector.veil.device_info.opengl.feature_flag.transform_feedback", VeilRenderSystem.transformFeedbackSupported(), "Whether transform feedback can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.texture_multi_bind", VeilRenderSystem.textureMultibindSupported(), "Whether glBindTextures can be used instead of glBindTexture");
        flagText("inspector.veil.device_info.opengl.feature_flag.sparse_buffers", VeilRenderSystem.sparseBuffersSupported(), "Whether sparse buffers can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.direct_state_access", VeilRenderSystem.directStateAccessSupported(), "Whether direct state accesss can be used");
        ImGui.separator();

        GLCapabilities caps = GL.getCapabilities();
        ImGui.popStyleColor();
        for (Map.Entry<Integer, Component> entry : SHADER_TYPES.entrySet()) {
            if (ImGui.collapsingHeader(entry.getValue().getString())) {
                ImGui.pushID(entry.getKey());
                ImGui.indent();
                ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);

                VeilShaderLimits limits = VeilRenderSystem.shaderLimits(entry.getKey());
                text("inspector.veil.device_info.opengl.shader.max_uniform_components", limits.maxUniformComponents(), "This is the number of active components of uniform variables that can be defined outside of a uniform block. The term \"component\" is meant as the basic component of a vector/matrix. So a vec3 takes up 3 components. The minimum value here is 1024, enough room for 256 vec4s.");
                text("inspector.veil.device_info.opengl.shader.max_uniform_blocks", limits.maxUniformBlocks(), "The maximum number of uniform blocks that this shader stage can access. The OpenGL-required minimum is 12 in GL 3.3, and 14 in GL 4.3.");
                if (entry.getKey() != GL_COMPUTE_SHADER) {
                    text("inspector.veil.device_info.opengl.shader.max_input_components", limits.maxInputComponents(), "The maximum number of components that this stage can take as input. The required minimum value differs from shader stage to shader stage.");
                    text("inspector.veil.device_info.opengl.shader.max_output_components", limits.maxOutputComponents(), "The maximum number of components that this stage can output. The required minimum value differs from shader stage to shader stage.");
                }
                text("inspector.veil.device_info.opengl.shader.max_texture_image_units", limits.maxTextureImageUnits(), "The maximum number of texture image units that the sampler in this shader can access. The OpenGL-required minimum value is 16 for each stage.");
                text("inspector.veil.device_info.opengl.shader.max_image_uniforms", limits.maxImageUniforms() > 0 ? limits.maxImageUniforms() : null, "The maximum number of image variables for this shader stage. The OpenGL-required minimum is 8 for fragment and compute shaders, and 0 for the rest. This means implementations may not allow you to use image variables in non-fragment or compute stages.");

                boolean atomicCounters = caps.OpenGL42 || caps.GL_ARB_shader_atomic_counters;
                text("inspector.veil.device_info.opengl.shader.max_atomic_counters", atomicCounters ? limits.maxAtomicCounters() : null, "The maximum number of Atomic Counter variables that this stage can define. The OpenGL-required minimum is 8 for fragment and compute shaders, and 0 for the rest.");
                text("inspector.veil.device_info.opengl.shader.max_atomic_counter_buffers", atomicCounters ? limits.maxAtomicCountBuffers() : null, "The maximum number of different buffers that the atomic counter variables can come from. The OpenGL-required minimum is 1 for fragment shaders, 8 for compute shaders (note: possible spec typo), and again 0 for the rest.");
                text("inspector.veil.device_info.opengl.shader.max_shader_storage_blocks", caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object ? limits.maxShaderStorageBlocks() : null, "The maximum number of different shader storage blocks that a stage can use. For fragment and compute shaders, the OpenGL-required minimum is 8; for the rest, it is 0.");

                ImGui.popStyleColor();
                ImGui.unindent();
                ImGui.popID();
            }
        }

        title(GL_UNIFORM);
        ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);
        text("inspector.veil.device_info.opengl.uniform.max_uniform_buffer_bindings", VeilRenderSystem.maxUniformBuffersBindings(), "The limit on the number of uniform buffer binding points. This is the limit for glBindBufferRange when using GL_UNIFORM_BUFFER.");
        text("inspector.veil.device_info.opengl.uniform.max_combined_uniform_blocks", glGetInteger(GL_MAX_COMBINED_UNIFORM_BLOCKS), "The maximum number of uniform blocks that all of the active programs can use. If two (or more) shader stages use the same block, they count separately towards this limit.");
        text("inspector.veil.device_info.opengl.uniform.max_combined_texture_image_units", VeilRenderSystem.maxCombinedTextureUnits(), "The total number of texture units that can be used from all active programs. This is the limit on glActiveTexture(GL_TEXTURE0 + i) and glBindSampler.");
        ImGui.separator();

        title(GL_TRANSFORM_FEEDBACK);
        text("inspector.veil.device_info.opengl.transform_feedback.max_separate_attributes", glGetInteger(GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS), "When doing separate mode Transform Feedback, this is the maximum number of varying variables that can be captured.");
        text("inspector.veil.device_info.opengl.transform_feedback.max_separate_components", glGetInteger(GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS), "When doing separate mode Transform Feedback, this is the maximum number of components for a single varying variable (note that varyings can be arrays or structs) that can be captured.");
        text("inspector.veil.device_info.opengl.transform_feedback.max_interleaved_components", glGetInteger(GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS), "When doing interleaved Transform Feedback, this is the total number of components that can be captured within a single buffer.");
        text("inspector.veil.device_info.opengl.transform_feedback.max_buffers", VeilRenderSystem.transformFeedbackSupported() ? VeilRenderSystem.maxTransformFeedbackBindings() : null, "The maximum number of buffers that can be written to in transform feedback operations.");
        ImGui.separator();

        boolean atomicCounters = caps.OpenGL42 || caps.GL_ARB_shader_atomic_counters;
        title(GL_ATOMIC_COUNTER);
        text("inspector.veil.device_info.opengl.atomic_counter.max_buffer_bindings", atomicCounters ? VeilRenderSystem.maxAtomicCounterBufferBindings() : null, "The total number of atomic counter buffer binding points. This is the limit for glBindBufferRange when using GL_ATOMIC_COUNTER_BUFFER.");
        text("inspector.veil.device_info.opengl.atomic_counter.max_combined_buffers", atomicCounters ? glGetInteger(GL_MAX_COMBINED_ATOMIC_COUNTER_BUFFERS) : null, "The maximum number of atomic counter buffers variables across all active programs.");
        text("inspector.veil.device_info.opengl.atomic_counter.max_combined_counters", atomicCounters ? glGetInteger(GL_MAX_COMBINED_ATOMIC_COUNTERS) : null, "The maximum number of atomic counter variables across all active programs.");
        ImGui.separator();

        boolean shaderStorageBuffers = caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object;
        title(GL_SHADER_STORAGE);
        text("inspector.veil.device_info.opengl.shader_storage.max_bindings", atomicCounters ? VeilRenderSystem.maxShaderStorageBufferBindings() : null, "The total number of shader storage buffer binding points. This is the limit for glBindBufferRange when using GL_SHADER_STORAGE_BUFFER.");
        text("inspector.veil.device_info.opengl.shader_storage.max_combined_blocks", shaderStorageBuffers ? glGetInteger(GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS) : null, "The maximum number of shader storage blocks across all active programs. As with UBOs, blocks that are the same between stages are counted for each stage.");
        text("inspector.veil.device_info.opengl.shader_storage.max_output_resources", shaderStorageBuffers ? glGetInteger(GL_MAX_COMBINED_SHADER_OUTPUT_RESOURCES) : null, "The total number of shader storage blocks, image variables, and fragment shader outputs across all active programs cannot exceed this number. This is the \"amount of stuff\" that a sequence of shaders can write to (barring Transform Feedback).");
        ImGui.separator();

        title(GL_TEXTURE);
        text("inspector.veil.device_info.opengl.texture.max_texture_size", RenderSystem.maxSupportedTextureSize(), null);
        text("inspector.veil.device_info.opengl.texture.max_array_texture_layers", VeilRenderSystem.maxArrayTextureLayers(), null);

        title(GL_FRAMEBUFFER);
        text("inspector.veil.device_info.opengl.framebuffer.max_color_attachments", VeilRenderSystem.maxColorAttachments(), null);
        text("inspector.veil.device_info.opengl.framebuffer.max_samples", VeilRenderSystem.maxSamples(), null);
    }

    private void renderOpenAL() {
        ImGui.pushStyleColor(ImGuiCol.Text, 0xFFFFFFFF);
        text("inspector.veil.device_info.openal.vendor", alGetString(AL_VENDOR), null);
        text("inspector.veil.device_info.openal.renderer", alGetString(AL_RENDERER), null);
        text("inspector.veil.device_info.openal.version", alGetString(AL_VERSION), null);
        ImGui.popStyleColor();
    }

    private void renderOpenCL() {
        VeilOpenCL cl = VeilOpenCL.get();
        VeilOpenCL.PlatformInfo[] platforms = cl.getPlatforms();

        title(CL_PLATFORMS);
        for (int i = 0; i < platforms.length; i++) {
            VeilOpenCL.PlatformInfo platform = platforms[i];
            if (!ImGui.collapsingHeader(I18n.get("inspector.veil.device_info.opencl.platform", platform.name(), platform.id()), i == 0 ? ImGuiTreeNodeFlags.DefaultOpen : 0)) {
                continue;
            }

            ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);
            text("inspector.veil.device_info.opencl.profile", platform.profile(), null);
            text("inspector.veil.device_info.opencl.cl_version", platform.version(), null);
            text("inspector.veil.device_info.opencl.vendor", platform.vendor(), null);
            ImGui.popStyleColor();

            ImGui.separator();

            VeilOpenCL.DeviceInfo[] devices = platform.devices();
            title(CL_DEVICES);
            for (int j = 0; j < devices.length; j++) {
                VeilOpenCL.DeviceInfo device = devices[j];
                if (!ImGui.collapsingHeader(I18n.get("inspector.veil.device_info.opencl.device", device.name(), device.id()), j == 0 ? ImGuiTreeNodeFlags.DefaultOpen : 0)) {
                    continue;
                }

                ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);
                ImGui.indent();
                List<Component> types = new ArrayList<>(1);
                if (device.isDefault()) {
                    types.add(CL_DEVICE_DEFAULT);
                }
                if (device.isCpu()) {
                    types.add(CL_DEVICE_CPU);
                }
                if (device.isGpu()) {
                    types.add(CL_DEVICE_GPU);
                }
                if (device.isAccelerator()) {
                    types.add(CL_DEVICE_ACCELERATOR);
                }
                text("inspector.veil.device_info.opencl.device.type", ComponentUtils.formatList(types, Component.literal(", ")), null);
                text("inspector.veil.device_info.opencl.device.vendor_id", "0x%X".formatted(device.vendorId()), null);
                text("inspector.veil.device_info.opencl.device.max_compute_units", device.maxComputeUnits(), null);
                text("inspector.veil.device_info.opencl.device.max_work_item_dimensions", device.maxWorkItemDimensions(), null);
                text("inspector.veil.device_info.opencl.device.max_work_group_size", device.maxWorkGroupSize(), null);
                text("inspector.veil.device_info.opencl.device.max_clock_frequency", device.maxClockFrequency(), null);
                text("inspector.veil.device_info.opencl.device.address_size", device.addressBits(), null);
                flagText("inspector.veil.device_info.opencl.device.available", device.available(), null);
                flagText("inspector.veil.device_info.opencl.device.compiler_available", device.compilerAvailable(), null);
                flagText("inspector.veil.device_info.opencl.device.require_manual_sync", device.requireManualInteropSync(), null);
                text("inspector.veil.device_info.opencl.device.vendor", device.vendor(), null);
                text("inspector.veil.device_info.opencl.device.version", device.version(), null);
                text("inspector.veil.device_info.opencl.device.driver_version", device.driverVersion(), null);
                text("inspector.veil.device_info.opencl.device.profile", device.profile(), null);
                text("inspector.veil.device_info.opencl.device.c_version", device.openclCVersion(), null);
                ImGui.unindent();
                ImGui.popStyleColor();
            }
        }
    }

    public static Component getShaderName(int shader) {
        return SHADER_TYPES.get(shader);
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public Component getGroup() {
        return INFO_GROUP;
    }

    @Override
    protected void renderComponents() {
        if (ImGui.beginTabBar("##info")) {
            if (ImGui.beginTabItem(I18n.get("inspector.veil.device_info.opencl"))) {
                this.renderOpenCL();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem(I18n.get("inspector.veil.device_info.opengl"))) {
                ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);
                this.renderOpenGL();
                ImGui.popStyleColor();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem(I18n.get("inspector.veil.device_info.openal"))) {
                ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);
                this.renderOpenAL();
                ImGui.popStyleColor();
                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowSizeConstraints(400, 460, Float.MAX_VALUE, Float.MAX_VALUE);
        super.render();
    }
}
