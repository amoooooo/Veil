package foundry.veil.impl.resource;

import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.impl.resource.action.OverrideAction;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiDragDropFlags;
import imgui.flag.ImGuiStyleVar;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

@ApiStatus.Internal
public class VeilResourceRenderer {

    private static final Component COPY_PATH = Component.translatable("resource.veil.action.copy_path");
    private static final Component OPEN_FOLDER = Component.translatable("resource.veil.action.open_folder");

    /**
     * Renders the filename of a resource, with drag-n-drop and context menu support
     *
     * @param resource The resource to render
     * @param fullName Whether to render the location of the resource
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void renderFilename(VeilResource<?> resource, boolean fullName) {
        ImGui.pushID(resource.hashCode());
        ImGui.beginGroup();
        resource.render(false, fullName);

        if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceAllowNullID)) {
            ImGui.setDragDropPayload("VEIL_RESOURCE", resource, ImGuiCond.Once);
            resource.render(true, fullName);
            ImGui.endDragDropSource();
        }
        ImGui.endGroup();
        ImGui.popID();


        VeilResourceInfo info = resource.resourceInfo();

        if (ImGui.beginPopupContextItem("" + info.location())) {
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 2f, 2f);
            ObjectArrayList<VeilResourceAction<?>> actions = new ObjectArrayList<>(resource.getActions());
            actions.addFirst(new OverrideAction<>());

            if (ImGui.selectable("##copy_path")) {
                ImGui.setClipboardText(info.location().toString());
            }

            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
            ImGui.setItemAllowOverlap();
            ImGui.sameLine();
            VeilImGuiUtil.icon(0xEB91);
            ImGui.sameLine();
            ImGui.popStyleVar();
            VeilImGuiUtil.component(COPY_PATH);

            ImGui.beginDisabled(info.isStatic());
            if (ImGui.selectable("##open_folder")) {
                Path file = info.modResourcePath() != null ? info.modResourcePath() : info.filePath();
                if (file.getParent() != null) {
                    Util.getPlatform().openFile(file.getParent().toFile());
                }
            }

            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
            ImGui.setItemAllowOverlap();
            ImGui.sameLine();
            VeilImGuiUtil.icon(0xECAF);
            ImGui.sameLine();
            ImGui.popStyleVar();
            VeilImGuiUtil.component(OPEN_FOLDER);
            ImGui.endDisabled();

            for (int i = 0; i < actions.size(); i++) {
                VeilResourceAction action = actions.get(i);
                if (ImGui.selectable("##action" + i)) {
                    action.perform(VeilRenderSystem.renderer().getEditorManager(), resource);
                }

                ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
                ImGui.setItemAllowOverlap();
                ImGui.sameLine();
                action.getIcon().ifPresent(icon -> {
                    VeilImGuiUtil.icon(icon);
                    ImGui.sameLine();
                });
                ImGui.popStyleVar();
                VeilImGuiUtil.component(action.getName());
            }

            ImGui.popStyleVar();
            ImGui.endPopup();
        }
    }
}
