package foundry.veil.api.resource.type;

import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.impl.resource.action.TextEditAction;
import imgui.extension.texteditor.TextEditorLanguageDefinition;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public record VeilShaderDefinitionResource(VeilResourceInfo resourceInfo,
                                           ShaderManager shaderManager) implements VeilShaderResource<VeilShaderDefinitionResource> {

//    @Override
//    public void render(boolean dragging, boolean fullName) {
//        ShaderManager shaderManager = VeilRenderSystem.renderer().getShaderManager();
//        ShaderProgram shader = shaderManager.getShader(shaderManager.getSourceSet().getShaderDefinitionLister().fileToId(this.resourceInfo.location()));
//        if (shader != null && shader.getFormat() != null && shader.getFormat().contains(VertexFormatElement.POSITION)) {
//            float size = ImGui.getTextLineHeight();
//            if (dragging) {
//                VeilShaderDefinitionResourceRenderer.render(shader, size * 8, size * 8);
//                VeilImGuiUtil.resourceLocation(this.resourceInfo().location());
//                return;
//            }
//
//            VeilShaderResource.super.render(false, fullName);
//            if (ImGui.isItemHovered()) {
//                ImGui.beginTooltip();
//                VeilShaderDefinitionResourceRenderer.render(shader, size * 16, size * 16);
//                ImGui.endTooltip();
//            }
//            return;
//        }
//
//        VeilShaderResource.super.render(dragging, fullName);
//    }

    @Override
    public List<VeilResourceAction<VeilShaderDefinitionResource>> getActions() {
        return List.of(new TextEditAction<>());
    }

    @Override
    public boolean canHotReload() {
        return true;
    }

    @Override
    public void hotReload(VeilResourceManager resourceManager) throws IOException {
        this.shaderManager.scheduleRecompile(this.shaderManager.getSourceSet().getShaderDefinitionLister().fileToId(this.resourceInfo.location()));
    }

    @Override
    public int getIconCode() {
        return 0xED0F; // Text file icon
    }

    @Override
    public @Nullable TextEditorLanguageDefinition languageDefinition() {
        return null;
    }
}
