package foundry.veil.impl.client.editor;

import foundry.veil.Veil;
import foundry.veil.VeilClient;
import foundry.veil.api.client.editor.SingleWindowEditor;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.resource.*;
import foundry.veil.impl.resource.VeilPackResources;
import foundry.veil.impl.resource.VeilResourceManagerImpl;
import foundry.veil.impl.resource.VeilResourceRenderer;
import foundry.veil.impl.resource.action.OverrideAction;
import foundry.veil.impl.resource.tree.VeilResourceFolder;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImString;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

@ApiStatus.Internal
public class ResourceManagerEditor extends SingleWindowEditor {

    public static final float ITEM_VERTICAL_PADDING = 3.0f;
    public static final Component TITLE = Component.translatable("editor.veil.resource.title");
    private float cellHeight = 0.0f;
    private VeilResource<?> contextResource;
    private List<VeilResourceAction<?>> actions;

    private CompletableFuture<?> reloadFuture;
    private final ImString searchText = new ImString();


    @Override
    public void renderComponents() {
        this.contextResource = null;
        this.actions = Collections.emptyList();

        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() - 100f);
        // TODO: Translation key
        ImGui.inputTextWithHint("##search", "Search", searchText);
        ImGui.sameLine();

        ImGui.pushFont(VeilRenderSystem.renderer().getEditorManager().getFont(VeilImGuiUtil.ICON_FONT, false, false));

        // Add button
        ImGui.setNextItemWidth(44f);
        if (ImGui.button(("" + (char) 0xED59))) {

        }

        ImGui.sameLine();

        // Reload button
        ImGui.setNextItemWidth(44f);
        ImGui.beginDisabled(this.reloadFuture != null && !this.reloadFuture.isDone());
        if (ImGui.button(("" + (char) 0xF33F))) {
            this.reloadFuture = Minecraft.getInstance().reloadResourcePacks();
        }
        ImGui.endDisabled();

        ImGui.popFont();


        VeilResourceManagerImpl resourceManager = VeilClient.resourceManager();
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0xFF0000FF);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 1f, ITEM_VERTICAL_PADDING);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 3f, 0f);

        this.cellHeight = ImGui.getTextLineHeight() + 2.0f * ITEM_VERTICAL_PADDING;

        if (ImGui.beginListBox("##file_tree", ImGui.getContentRegionAvailX(), ImGui.getContentRegionAvailY())) {
            if (searchText.getLength() == 0) {
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
                }
            } else {
                // find resources that meet the search text and ONLY render those
                Queue<VeilResourceFolder> folders = new ArrayDeque<>();
                ObjectArrayList<VeilResource<?>> resources = new ObjectArrayList<>();

                for (VeilPackResources pack : resourceManager.getAllPacks()) {
                    folders.add(pack.getRoot());
                }

                while (!folders.isEmpty()) {
                    VeilResourceFolder folder = folders.poll();

                    for (VeilResource<?> resource : folder.getResources()) {
                        if (resource.resourceInfo().location().toString().contains(this.searchText.get())) {
                            resources.add(resource);
                        }
                    }

                    folders.addAll(folder.getSubFolders());
                }

                ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 4f, 4f);
                for (VeilResource<?> resource : resources) {
                    float startX = ImGui.getCursorScreenPosX();
                    ImGui.selectable("##" + resource.resourceInfo().location(), false, ImGuiSelectableFlags.AllowItemOverlap, ImGui.getContentRegionAvailX(), 22f);

                    ImGui.setItemAllowOverlap();
                    ImGui.sameLine();
                    ImGui.setCursorScreenPos(startX, ImGui.getCursorScreenPosY());
                    VeilResourceRenderer.renderFilename(resource, true);

                }
                ImGui.popStyleVar();

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
//        VeilImGuiUtil.icon(open ? 0xED6F : 0xF43B);
        ImGui.text("/");
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
            ImGui.selectable("##" + resource.resourceInfo().location(), false, ImGuiSelectableFlags.AllowItemOverlap, ImGui.getContentRegionAvailX(), cellHeight);

            ImGui.setItemAllowOverlap();
            ImGui.sameLine();

            ImVec2 selectableCursorScreenPos = ImGui.getCursorScreenPos();

            float shift = (this.cellHeight - ImGui.getTextLineHeight()) / 2.0f;
            ImGui.setCursorScreenPos(startX, selectableCursorScreenPos.y + shift);
            VeilResourceRenderer.renderFilename(resource, false);
            ImGui.setCursorScreenPos(startX, ImGui.getCursorScreenPosY() - shift);


            count++;
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
