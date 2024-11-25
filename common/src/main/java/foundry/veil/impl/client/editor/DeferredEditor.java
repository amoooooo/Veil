package foundry.veil.impl.client.editor;

import foundry.veil.api.client.editor.SingleWindowEditor;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.deferred.VeilDeferredRenderer;
import foundry.veil.api.client.render.deferred.light.renderer.LightRenderer;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import imgui.ImGui;
import imgui.type.ImBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class DeferredEditor extends SingleWindowEditor {

    public static final Component TITLE = Component.translatable("editor.veil.deferred.title");

    private static final Component ENABLE_PIPELINE = Component.translatable("editor.veil.deferred.toggle.pipeline");
    // TODO move to light editor
    private static final Component ENABLE_AO = Component.translatable("editor.veil.deferred.toggle.ao");
    private static final Component ENABLE_VANILLA_LIGHT = Component.translatable("editor.veil.deferred.toggle.vanilla_light");
    private static final Component ENABLE_VANILLA_ENTITY_LIGHT = Component.translatable("editor.veil.deferred.toggle.vanilla_entity_light");

    private final ImBoolean enableDeferredPipeline = new ImBoolean();
    private final ImBoolean enableAmbientOcclusion = new ImBoolean();
    private final ImBoolean enableVanillaLight = new ImBoolean();
    private final ImBoolean enableEntityLight = new ImBoolean();

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public Component getGroup() {
        return DEFERRED_GROUP;
    }

    @Override
    protected void renderComponents() {
        VeilRenderer renderer = VeilRenderSystem.renderer();
        ShaderPreDefinitions definitions = renderer.getShaderDefinitions();
        VeilDeferredRenderer deferredRenderer = renderer.getDeferredRenderer();
        LightRenderer lightRenderer =  VeilRenderSystem.renderer().getLightRenderer();

        this.enableDeferredPipeline.set(deferredRenderer.getRendererState() != VeilDeferredRenderer.RendererState.DISABLED);
        if (ImGui.checkbox(ENABLE_PIPELINE.getString(), this.enableDeferredPipeline)) {
            if (this.enableDeferredPipeline.get()) {
                deferredRenderer.enable();
            } else {
                deferredRenderer.disable();
            }
            Minecraft.getInstance().levelRenderer.allChanged();
        }

        ImGui.sameLine();
        this.enableAmbientOcclusion.set(lightRenderer.isAmbientOcclusionEnabled());
        if (ImGui.checkbox(ENABLE_AO.getString(), this.enableAmbientOcclusion)) {
            if (this.enableAmbientOcclusion.get()) {
                lightRenderer.enableAmbientOcclusion();
            } else {
                lightRenderer.disableAmbientOcclusion();
            }
        }

//        ImGui.sameLine();
//        this.enableVanillaLight.set(lightRenderer.isVanillaLightEnabled());
//        if (ImGui.checkbox(ENABLE_VANILLA_LIGHT.getString(), this.enableVanillaLight)) {
//            if (this.enableVanillaLight.get()) {
//                lightRenderer.enableVanillaLight();
//            } else {
//                lightRenderer.disableVanillaLight();
//            }
//        }
//
//        ImGui.sameLine();
//        this.enableEntityLight.set(definitions.getDefinition(VeilDeferredRenderer.DISABLE_VANILLA_ENTITY_LIGHT_KEY) == null);
//        if (ImGui.checkbox(ENABLE_VANILLA_ENTITY_LIGHT.getString(), this.enableEntityLight)) {
//            if (this.enableEntityLight.get()) {
//                definitions.remove(VeilDeferredRenderer.DISABLE_VANILLA_ENTITY_LIGHT_KEY);
//            } else {
//                definitions.define(VeilDeferredRenderer.DISABLE_VANILLA_ENTITY_LIGHT_KEY);
//            }
//        }
//
//        VeilImGuiUtil.component(FramebufferEditor.TITLE);
//        if (ImGui.beginTabBar("##framebuffers")) {
//            FramebufferEditor.drawBuffers(VeilFramebuffers.OPAQUE, null);
//            FramebufferEditor.drawBuffers(VeilFramebuffers.OPAQUE_LIGHT, null);
//            FramebufferEditor.drawBuffers(VeilFramebuffers.OPAQUE_FINAL, null);
//            FramebufferEditor.drawBuffers(VeilFramebuffers.TRANSPARENT, null);
//            FramebufferEditor.drawBuffers(VeilFramebuffers.TRANSPARENT_LIGHT, null);
//            FramebufferEditor.drawBuffers(VeilFramebuffers.TRANSPARENT_FINAL, null);
//            ImGui.endTabBar();
//        }
    }

    @Override
    public boolean isEnabled() {
        return VeilRenderSystem.renderer().getDeferredRenderer().isEnabled();
    }
}
