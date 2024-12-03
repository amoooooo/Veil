package foundry.veil.impl.client.editor;

import foundry.veil.VeilClient;
import foundry.veil.api.client.editor.SingleWindowEditor;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.resource.*;
import foundry.veil.impl.resource.VeilPackResources;
import foundry.veil.impl.resource.VeilResourceManagerImpl;
import foundry.veil.impl.resource.VeilResourceRenderer;
import foundry.veil.impl.resource.tree.VeilResourceFolder;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTreeNodeFlags;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ApiStatus.Internal
public class ResourceManagerEditor extends SingleWindowEditor {

    private float cellHeight = 0.0f;
    public static final float ITEM_VERTICAL_PADDING = 5.0f;
    public static final Component TITLE = Component.translatable("editor.veil.resource.title");

    private static final Component RELOAD_BUTTON = Component.translatable("editor.veil.resource.button.reload");
    private static final Component COPY_PATH = Component.translatable("editor.veil.resource.action.copy_path");
    private static final Component OPEN_FOLDER = Component.translatable("editor.veil.resource.action.open_folder");

    private VeilResource<?> contextResource;
    private List<? extends VeilResourceAction<?>> actions;

    private CompletableFuture<?> reloadFuture;

    @Override
    public void renderComponents() {
        this.contextResource = null;
        this.actions = Collections.emptyList();

        ImGui.beginDisabled(this.reloadFuture != null && !this.reloadFuture.isDone());
        if (ImGui.button(RELOAD_BUTTON.getString())) {
            this.reloadFuture = Minecraft.getInstance().reloadResourcePacks();
        }
        ImGui.endDisabled();

        VeilResourceManagerImpl resourceManager = VeilClient.resourceManager();
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0xFF0000FF);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 1f, ITEM_VERTICAL_PADDING);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 3f, 0f);

        this.cellHeight = ImGui.getTextLineHeight() + 2.0f * ITEM_VERTICAL_PADDING;

        if (ImGui.beginListBox("##file_tree", ImGui.getContentRegionAvailX(), ImGui.getContentRegionAvailY())) {
            List<VeilPackResources> packs = resourceManager.getAllPacks();
            for (int i = packs.size() - 1; i >= 0; i--) {
                VeilPackResources pack = packs.get(i);
                String modid = pack.getName();
                int color = VeilImGuiUtil.colorOf(modid);

                boolean open = ImGui.treeNodeEx("##" + modid, ImGuiTreeNodeFlags.SpanAvailWidth | ImGuiTreeNodeFlags.FramePadding);


                ImGui.pushStyleColor(ImGuiCol.Text, color);
                ImGui.sameLine();
                int icon = pack.getTexture();

                ImVec2 cursorScreenPos = ImGui.getCursorScreenPos();
                VeilImGuiUtil.icon(0xF523, color);

                if (icon != 0) {
                    float size = ImGui.getTextLineHeight();

                    float minX = cursorScreenPos.x;
                    float minY = cursorScreenPos.y + ITEM_VERTICAL_PADDING + 1.0f;

                    ImGui.getWindowDrawList().addRectFilled(minX, minY, minX + size, minY + size, 0xff000000);
                    ImGui.getWindowDrawList().addImage(icon, minX, minY, minX + size, minY + size);
                }

                ImGui.sameLine();
                ImGui.text(modid);
                ImGui.popStyleColor();

                if (open) {
                    this.renderFolderContents(pack.getRoot());
                    ImGui.treePop();
                }

//                ImGui.separator();
            }
            ImGui.endListBox();
        }
        ImGui.popStyleVar();
        ImGui.popStyleVar();
        ImGui.popStyleColor();

    }

    private int renderFolder(VeilResourceFolder folder) {
        boolean open = ImGui.treeNodeEx("##" + folder.getName(), ImGuiTreeNodeFlags.SpanAvailWidth | ImGuiTreeNodeFlags.FramePadding);
        ImGui.sameLine();

        ImGui.beginDisabled();
        VeilImGuiUtil.icon(open ? 0xED6F : 0xF43B);
        ImGui.endDisabled();

        ImGui.sameLine();
        ImGui.text(folder.getName());

        int count = 1;

        if (open) {
            count += this.renderFolderContents(folder);
            ImGui.treePop();
        }

        return count;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int renderFolderContents(VeilResourceFolder folder) {
        int count = 0;

        ImVec2 cursorScreenPos = ImGui.getCursorScreenPos();

        for (VeilResourceFolder subFolder : folder.getSubFolders()) {
            count += this.renderFolder(subFolder);
        }

        ImGui.indent();
        for (VeilResource<?> resource : folder.getResources()) {
            VeilResourceInfo info = resource.resourceInfo();
            if (info.hidden()) {
                continue;
            }

            float startX = ImGui.getCursorScreenPosX();
            float cellHeight = this.cellHeight;
            ImGui.selectable("##" + resource.resourceInfo().location(), false, ImGuiSelectableFlags.AllowItemOverlap, ImGui.getContentRegionAvailX(), cellHeight);

            ImGui.setItemAllowOverlap();
            ImGui.sameLine();

            ImVec2 selectableCursorScreenPos = ImGui.getCursorScreenPos();

            float shift = (cellHeight - ImGui.getTextLineHeight()) / 2.0f;
            ImGui.setCursorScreenPos(startX, selectableCursorScreenPos.y + shift);
            VeilResourceRenderer.renderFilename(resource);
            ImGui.setCursorScreenPos(startX, ImGui.getCursorScreenPosY() - shift);

            if (ImGui.beginPopupContextItem("" + resource.resourceInfo().location())) {
                if (resource != this.contextResource) {
                    this.contextResource = resource;
                    this.actions = resource.getActions();
                }

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

                for (int i = 0; i < this.actions.size(); i++) {
                    VeilResourceAction action = this.actions.get(i);
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

                ImGui.endPopup();
            }
            count ++;
        }

        float lineX = cursorScreenPos.x - 4.0f;
        ImGui.getWindowDrawList().addRectFilled(lineX, cursorScreenPos.y, lineX + 1.5f, cursorScreenPos.y + count * cellHeight, 0x22FFFFFF);
        ImGui.unindent();

        return count;
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public Component getGroup() {
        return RESOURCE_GROUP;
    }
}
