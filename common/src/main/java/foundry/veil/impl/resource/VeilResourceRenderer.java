package foundry.veil.impl.resource;

import foundry.veil.api.resource.VeilResource;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiDragDropFlags;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class VeilResourceRenderer {

    public static void renderFilename(VeilResource<?> resource) {
        ImGui.pushID(resource.hashCode());
        ImGui.beginGroup();
        resource.render(false);

        if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceAllowNullID)) {
            ImGui.setDragDropPayload("VEIL_RESOURCE", resource, ImGuiCond.Once);
            resource.render(true);
            ImGui.endDragDropSource();
        }
        ImGui.endGroup();
        ImGui.popID();
    }
}
