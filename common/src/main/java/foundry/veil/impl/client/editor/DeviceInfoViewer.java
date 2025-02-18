package foundry.veil.impl.client.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.editor.SingleWindowInspector;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilShaderLimits;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
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
    private static final Component NO = CommonComponents.GUI_NO.copy().withStyle(style -> style.withColor(0xFFFF0000));

    private static final Component GL_FEATURE_FLAG = Component.translatable("inspector.veil.device_info.opengl.feature_flag");
    private static final Component GL_VERTEX_ARRAY = Component.translatable("inspector.veil.device_info.opengl.vertex_array");
    private static final Component GL_UNIFORM = Component.translatable("inspector.veil.device_info.opengl.uniform");
    private static final Component GL_TRANSFORM_FEEDBACK = Component.translatable("inspector.veil.device_info.opengl.transform_feedback");
    private static final Component GL_ATOMIC_COUNTER = Component.translatable("inspector.veil.device_info.opengl.atomic_counter");
    private static final Component GL_SHADER_STORAGE = Component.translatable("inspector.veil.device_info.opengl.shader_storage");
    private static final Component GL_TEXTURE = Component.translatable("inspector.veil.device_info.opengl.texture");
    private static final Component GL_FRAMEBUFFER = Component.translatable("inspector.veil.device_info.opengl.framebuffer");

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

    private static void text(String key, @Nullable String tooltip, @Nullable Object... values) {
        if (values != null) {
            Object[] valueComponents = Arrays.stream(values)
                    .filter(Objects::nonNull)
                    .map(value -> value instanceof MutableComponent c ? c.withStyle(style -> style.withColor(0xFFFFFFFF)) : Component.literal(value.toString()).withStyle(style -> style.withColor(0xFFFFFFFF)))
                    .toArray();
            if (valueComponents.length == 0) {
                VeilImGuiUtil.component(Component.translatable(key, UNSUPPORTED).withStyle(style -> style.withColor(VeilImGuiUtil.getColor(ImGuiCol.TextDisabled))));
            } else {
                VeilImGuiUtil.component(Component.translatable(key, valueComponents));
            }
        } else {
            VeilImGuiUtil.component(Component.translatable(key, UNSUPPORTED).withStyle(style -> style.withColor(VeilImGuiUtil.getColor(ImGuiCol.TextDisabled))));
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
        text("inspector.veil.device_info.opengl.vendor", null, glGetString(GL_VENDOR));
        text("inspector.veil.device_info.opengl.renderer", null, glGetString(GL_RENDERER));
        text("inspector.veil.device_info.opengl.version", null, glGetString(GL_VERSION));
        ImGui.popStyleColor();
        ImGui.separator();

        title(GL_FEATURE_FLAG);
        flagText("inspector.veil.device_info.opengl.feature_flag.compute", VeilRenderSystem.computeSupported(), "Whether compute shaders can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.atomic_counter", VeilRenderSystem.atomicCounterSupported(), "Whether atomic counters can be used in shaders");
        flagText("inspector.veil.device_info.opengl.feature_flag.transform_feedback", VeilRenderSystem.transformFeedbackSupported(), "Whether transform feedback can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.multi_bind", VeilRenderSystem.multibindSupported(), "Whether glBindTextures can be used instead of glBindTexture");
        flagText("inspector.veil.device_info.opengl.feature_flag.sparse_buffers", VeilRenderSystem.sparseBuffersSupported(), "Whether sparse buffers can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.direct_state_access", VeilRenderSystem.directStateAccessSupported(), "Whether direct state accesss can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.separate_shader_objects", VeilRenderSystem.separateShaderObjectsSupported(), "Whether program pipelines can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.clear_texture", VeilRenderSystem.clearTextureSupported(), "Whether glClearTexImage can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.copy_image", VeilRenderSystem.copyImageSupported(), "Whether glCopyImageSubData can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.shader_storage_buffers", VeilRenderSystem.shaderStorageBufferSupported(), "Whether shader storage buffers can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.program_interface_query", VeilRenderSystem.programInterfaceQuerySupported(), "Whether the new style program interface query can be used");
        flagText("inspector.veil.device_info.opengl.feature_flag.texture_anisotropy", VeilRenderSystem.textureAnisotropySupported(), "Whether GL_TEXTURE_MAX_ANISOTROPY can be set as a texture parameter");
        flagText("inspector.veil.device_info.opengl.feature_flag.texture_mirror_clamp_to_edge", VeilRenderSystem.textureMirrorClampToEdgeSupported(), "Whether GL_MIRROR_CLAMP_TO_EDGE can be set as a texture edge value option");
        flagText("inspector.veil.device_info.opengl.feature_flag.texture_cube_map_seamless", VeilRenderSystem.textureCubeMapSeamlessSupported(), "Whether GL_TEXTURE_CUBE_MAP_SEAMLESS can be set as a texture parameter");
        flagText("inspector.veil.device_info.opengl.feature_flag.nv_draw_texture", VeilRenderSystem.nvDrawTextureSupported(), "Whether glDrawTextureNV can be used");
        ImGui.separator();

        GLCapabilities caps = GL.getCapabilities();
        ImGui.popStyleColor();
        for (Map.Entry<Integer, Component> entry : SHADER_TYPES.entrySet()) {
            if (ImGui.collapsingHeader(entry.getValue().getString())) {
                ImGui.pushID(entry.getKey());
                ImGui.indent();
                ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);

                VeilShaderLimits limits = VeilRenderSystem.shaderLimits(entry.getKey());
                text("inspector.veil.device_info.opengl.shader.max_uniform_components", "This is the number of active components of uniform variables that can be defined outside of a uniform block. The term \"component\" is meant as the basic component of a vector/matrix. So a vec3 takes up 3 components. The minimum value here is 1024, enough room for 256 vec4s.", limits.maxUniformComponents());
                text("inspector.veil.device_info.opengl.shader.max_uniform_blocks", "The maximum number of uniform blocks that this shader stage can access. The OpenGL-required minimum is 12 in GL 3.3, and 14 in GL 4.3.", limits.maxUniformBlocks());
                if (entry.getKey() != GL_COMPUTE_SHADER) {
                    text("inspector.veil.device_info.opengl.shader.max_input_components", "The maximum number of components that this stage can take as input. The required minimum value differs from shader stage to shader stage.", limits.maxInputComponents());
                    text("inspector.veil.device_info.opengl.shader.max_output_components", "The maximum number of components that this stage can output. The required minimum value differs from shader stage to shader stage.", limits.maxOutputComponents());
                }
                text("inspector.veil.device_info.opengl.shader.max_texture_image_units", "The maximum number of texture image units that the sampler in this shader can access. The OpenGL-required minimum value is 16 for each stage.", limits.maxTextureImageUnits());
                text("inspector.veil.device_info.opengl.shader.max_image_uniforms", "The maximum number of image variables for this shader stage. The OpenGL-required minimum is 8 for fragment and compute shaders, and 0 for the rest. This means implementations may not allow you to use image variables in non-fragment or compute stages.", limits.maxImageUniforms() > 0 ? limits.maxImageUniforms() : null);

                boolean atomicCounters = caps.OpenGL42 || caps.GL_ARB_shader_atomic_counters;
                text("inspector.veil.device_info.opengl.shader.max_atomic_counters", "The maximum number of Atomic Counter variables that this stage can define. The OpenGL-required minimum is 8 for fragment and compute shaders, and 0 for the rest.", atomicCounters ? limits.maxAtomicCounters() : null);
                text("inspector.veil.device_info.opengl.shader.max_atomic_counter_buffers", "The maximum number of different buffers that the atomic counter variables can come from. The OpenGL-required minimum is 1 for fragment shaders, 8 for compute shaders (note: possible spec typo), and again 0 for the rest.", atomicCounters ? limits.maxAtomicCountBuffers() : null);
                text("inspector.veil.device_info.opengl.shader.max_shader_storage_blocks", "The maximum number of different shader storage blocks that a stage can use. For fragment and compute shaders, the OpenGL-required minimum is 8; for the rest, it is 0.", caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object ? limits.maxShaderStorageBlocks() : null);

                ImGui.popStyleColor();
                ImGui.unindent();
                ImGui.popID();
            }
        }

        ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);

        title(GL_VERTEX_ARRAY);
        text("inspector.veil.device_info.opengl.vertex_array.max_vertex_attributes", null, VeilRenderSystem.maxVertexAttributes());
        text("inspector.veil.device_info.opengl.vertex_array.max_vertex_attribute_relative_offset", null, VeilRenderSystem.maxVertexAttributeRelativeOffset());
        ImGui.separator();

        title(GL_UNIFORM);
        text("inspector.veil.device_info.opengl.uniform.max_uniform_buffer_bindings", "The limit on the number of uniform buffer binding points. This is the limit for glBindBufferRange when using GL_UNIFORM_BUFFER.", VeilRenderSystem.maxUniformBuffersBindings());
        text("inspector.veil.device_info.opengl.uniform.max_uniform_buffer_size", null, VeilRenderSystem.maxUniformBufferSize());
        text("inspector.veil.device_info.opengl.uniform.uniform_buffer_alignment", "If you bind a uniform buffer with glBindBufferRange, the offset field of that parameter must be a multiple of GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT (this is a global value, not a per-program or per-block one). Thus, if you want to put the data for multiple uniform blocks in a single buffer object, you must make sure that the data for each within that block matches this alignment.", VeilRenderSystem.uniformBufferAlignment());
        text("inspector.veil.device_info.opengl.uniform.max_combined_uniform_blocks", "The maximum number of uniform blocks that all of the active programs can use. If two (or more) shader stages use the same block, they count separately towards this limit.", glGetInteger(GL_MAX_COMBINED_UNIFORM_BLOCKS));
        text("inspector.veil.device_info.opengl.uniform.max_combined_texture_image_units", "The total number of texture units that can be used from all active programs. This is the limit on glActiveTexture(GL_TEXTURE0 + i) and glBindSampler.", VeilRenderSystem.maxCombinedTextureUnits());
        ImGui.separator();

        title(GL_TRANSFORM_FEEDBACK);
        text("inspector.veil.device_info.opengl.transform_feedback.max_separate_attributes", "When doing separate mode Transform Feedback, this is the maximum number of varying variables that can be captured.", glGetInteger(GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS));
        text("inspector.veil.device_info.opengl.transform_feedback.max_separate_components", "When doing separate mode Transform Feedback, this is the maximum number of components for a single varying variable (note that varyings can be arrays or structs) that can be captured.", glGetInteger(GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS));
        text("inspector.veil.device_info.opengl.transform_feedback.max_interleaved_components", "When doing interleaved Transform Feedback, this is the total number of components that can be captured within a single buffer.", glGetInteger(GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS));
        text("inspector.veil.device_info.opengl.transform_feedback.max_buffers", "The maximum number of buffers that can be written to in transform feedback operations.", VeilRenderSystem.transformFeedbackSupported() ? VeilRenderSystem.maxTransformFeedbackBindings() : null);
        ImGui.separator();

        boolean atomicCounters = VeilRenderSystem.atomicCounterSupported();
        title(GL_ATOMIC_COUNTER);
        text("inspector.veil.device_info.opengl.atomic_counter.max_buffer_bindings", "The total number of atomic counter buffer binding points. This is the limit for glBindBufferRange when using GL_ATOMIC_COUNTER_BUFFER.", atomicCounters ? VeilRenderSystem.maxAtomicCounterBufferBindings() : null);
        text("inspector.veil.device_info.opengl.atomic_counter.max_combined_buffers", "The maximum number of atomic counter buffers variables across all active programs.", atomicCounters ? glGetInteger(GL_MAX_COMBINED_ATOMIC_COUNTER_BUFFERS) : null);
        text("inspector.veil.device_info.opengl.atomic_counter.max_combined_counters", "The maximum number of atomic counter variables across all active programs.", atomicCounters ? glGetInteger(GL_MAX_COMBINED_ATOMIC_COUNTERS) : null);
        ImGui.separator();

        boolean shaderStorageBuffers = VeilRenderSystem.shaderStorageBufferSupported();
        title(GL_SHADER_STORAGE);
        text("inspector.veil.device_info.opengl.shader_storage.max_bindings", "The total number of shader storage buffer binding points. This is the limit for glBindBufferRange when using GL_SHADER_STORAGE_BUFFER.", shaderStorageBuffers ? VeilRenderSystem.maxShaderStorageBufferBindings() : null);
        text("inspector.veil.device_info.opengl.shader_storage.max_size", null, shaderStorageBuffers ? VeilRenderSystem.maxShaderStorageBufferSize() : null);
        text("inspector.veil.device_info.opengl.shader_storage.max_combined_blocks", "The maximum number of shader storage blocks across all active programs. As with UBOs, blocks that are the same between stages are counted for each stage.", shaderStorageBuffers ? glGetInteger(GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS) : null);
        text("inspector.veil.device_info.opengl.shader_storage.max_output_resources", "The total number of shader storage blocks, image variables, and fragment shader outputs across all active programs cannot exceed this number. This is the \"amount of stuff\" that a sequence of shaders can write to (barring Transform Feedback).", shaderStorageBuffers ? glGetInteger(GL_MAX_COMBINED_SHADER_OUTPUT_RESOURCES) : null);
        ImGui.separator();

        title(GL_TEXTURE);
        text("inspector.veil.device_info.opengl.texture.max_texture_size", null, RenderSystem.maxSupportedTextureSize());
        text("inspector.veil.device_info.opengl.texture.max_array_texture_layers", null, VeilRenderSystem.maxArrayTextureLayers());
        text("inspector.veil.device_info.opengl.texture.max_texture_anisotropy", "The maximum value GL_TEXTURE_MAX_ANISOTROPY can be set to", VeilRenderSystem.textureAnisotropySupported() ? VeilRenderSystem.maxTextureAnisotropy() : null);

        title(GL_FRAMEBUFFER);
        text("inspector.veil.device_info.opengl.framebuffer.max_size", null, VeilRenderSystem.maxFramebufferWidth(), VeilRenderSystem.maxFramebufferHeight());
        text("inspector.veil.device_info.opengl.framebuffer.max_color_attachments", null, VeilRenderSystem.maxColorAttachments());
        text("inspector.veil.device_info.opengl.framebuffer.max_samples", null, VeilRenderSystem.maxSamples());
    }

    private void renderOpenAL() {
        ImGui.pushStyleColor(ImGuiCol.Text, 0xFFFFFFFF);
        text("inspector.veil.device_info.openal.vendor", null, alGetString(AL_VENDOR));
        text("inspector.veil.device_info.openal.renderer", null, alGetString(AL_RENDERER));
        text("inspector.veil.device_info.openal.version", null, alGetString(AL_VERSION));
        ImGui.popStyleColor();
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
        ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);
        this.renderOpenGL();
        ImGui.popStyleColor();
//        if (ImGui.beginTabBar("##info")) {
//            if (ImGui.beginTabItem(I18n.get("inspector.veil.device_info.opengl"))) {
//                ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);
//                this.renderOpenGL();
//                ImGui.popStyleColor();
//                ImGui.endTabItem();
//            }
//            if (ImGui.beginTabItem(I18n.get("inspector.veil.device_info.openal"))) {
//                ImGui.pushStyleColor(ImGuiCol.Text, TEXT_COLOR);
//                this.renderOpenAL();
//                ImGui.popStyleColor();
//                ImGui.endTabItem();
//            }
//            ImGui.endTabBar();
//        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowSizeConstraints(400, 460, Float.MAX_VALUE, Float.MAX_VALUE);
        super.render();
    }
}
