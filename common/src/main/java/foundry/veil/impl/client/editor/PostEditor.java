package foundry.veil.impl.client.editor;

import foundry.veil.Veil;
import foundry.veil.api.client.editor.SingleWindowEditor;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostProcessingManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiDataType;
import imgui.flag.ImGuiDragDropFlags;
import imgui.type.ImInt;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashSet;
import java.util.Set;

@ApiStatus.Internal
public class PostEditor extends SingleWindowEditor {

    public static final Component TITLE = Component.translatable("editor.veil.post.title");

    private static final Component INACTIVE = Component.translatable("editor.veil.post.inactive");
    private static final Component ACTIVE = Component.translatable("editor.veil.post.active");

    private final Set<ResourceLocation> removedPipelines;

    public PostEditor() {
        this.removedPipelines = new HashSet<>(1);
    }

    private static boolean isInternal(ResourceLocation id) {
        return Veil.MODID.equals(id.getNamespace()) && id.getPath().startsWith("core/");
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(600, 0);
        super.render();
    }

    @Override
    public void renderComponents() {
        this.removedPipelines.clear();
        PostProcessingManager postProcessingManager = VeilRenderSystem.renderer().getPostProcessingManager();

        float availableWidth = ImGui.getContentRegionAvailX();

        ImGui.setNextItemWidth(availableWidth / 2);
        ImGui.beginGroup();
        VeilImGuiUtil.component(INACTIVE);
        if (ImGui.beginListBox("##available_pipelines", availableWidth / 2, 0)) {
            for (ResourceLocation entry : postProcessingManager.getPipelines()) {
                if (postProcessingManager.isActive(entry) || isInternal(entry)) {
                    continue;
                }

                VeilImGuiUtil.resourceLocation(entry);

                if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceAllowNullID)) {
                    ImGui.setDragDropPayload("POST_PIPELINE", entry, ImGuiCond.Once);
                    VeilImGuiUtil.resourceLocation(entry);

                    ImGui.endDragDropSource();
                }
            }

            ImGui.endListBox();
        }

        if (ImGui.beginDragDropTarget()) {
            ResourceLocation payload = ImGui.acceptDragDropPayload("POST_PIPELINE");
            if (payload != null) {
                this.removedPipelines.add(payload);
            }

            ImGui.endDragDropTarget();
        }

        ImGui.endGroup();

        ImGui.sameLine();

        ImGui.setNextItemWidth(availableWidth / 2);
        ImGui.beginGroup();
        VeilImGuiUtil.component(ACTIVE);

        if (ImGui.beginListBox("##shaders", availableWidth / 2, 0)) {
            for (PostProcessingManager.ProfileEntry entry : postProcessingManager.getActivePipelines()) {
                ResourceLocation id = entry.getPipeline();
                if (isInternal(id)) {
                    continue;
                }

                ImGui.pushID(id.toString());
                VeilImGuiUtil.resourceLocation(id);
                if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceAllowNullID)) {
                    ImGui.setDragDropPayload("POST_PIPELINE", id, ImGuiCond.Once);
                    VeilImGuiUtil.resourceLocation(id);
                    ImGui.endDragDropSource();
                }

                float priorityWidth = Math.max(ImGui.calcTextSize("999999").x, ImGui.calcTextSize(Integer.toString(entry.getPriority())).x);
                ImGui.setItemAllowOverlap();
                ImGui.sameLine(ImGui.getContentRegionAvailX() - priorityWidth - 2);
                ImGui.setNextItemWidth(priorityWidth);
                ImInt editPriority = new ImInt(entry.getPriority());
                if (ImGui.dragScalar("##priority", ImGuiDataType.S32, editPriority, 1)) {
                    entry.setPriority(editPriority.get());
                }
                ImGui.popID();
            }

            ImGui.endListBox();
        }

        if (ImGui.beginDragDropTarget()) {
            ResourceLocation payload = ImGui.acceptDragDropPayload("POST_PIPELINE");
            if (payload != null) {
                postProcessingManager.add(payload);
            }

            ImGui.endDragDropTarget();
        }
        ImGui.endGroup();

        for (ResourceLocation id : this.removedPipelines) {
            postProcessingManager.remove(id);
        }
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public Component getGroup() {
        return RENDERER_GROUP;
    }
}
