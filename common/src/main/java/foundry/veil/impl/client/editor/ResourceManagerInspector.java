package foundry.veil.impl.client.editor;

import foundry.veil.VeilClient;
import foundry.veil.api.client.editor.SingleWindowInspector;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.impl.resource.VeilPackResources;
import foundry.veil.impl.resource.VeilResourceManagerImpl;
import foundry.veil.impl.resource.VeilResourceRenderer;
import foundry.veil.impl.resource.tree.VeilResourceFolder;
import imgui.ImGui;
import imgui.ImGuiListClipper;
import imgui.ImVec2;
import imgui.callback.ImListClipperCallback;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImString;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@ApiStatus.Internal
public class ResourceManagerInspector extends SingleWindowInspector {

    public static final float ITEM_VERTICAL_PADDING = 3.0f;
    public static final Component TITLE = Component.translatable("inspector.veil.resource.title");
    public static final Component SEARCH = Component.translatable("inspector.veil.resource.hint.search");
    public static final Component ADD_TOOLTIP = Component.translatable("inspector.veil.resource.button.add_pack");
    public static final Component RELOAD_TOOLTIP = Component.translatable("inspector.veil.resource.button.reload");
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
        ImGui.inputTextWithHint("##search", SEARCH.getString(), this.searchText);
        ImGui.sameLine();

        ImGui.pushFont(VeilRenderSystem.renderer().getEditorManager().getFont(VeilImGuiUtil.ICON_FONT, false, false));

        // Add button
        ImGui.beginDisabled();
        ImGui.setNextItemWidth(44f);
        if (ImGui.button(("" + (char) 0xED59))) {

        }
        ImGui.endDisabled();
        if (ImGui.isItemHovered()) {
            VeilImGuiUtil.setTooltip(ADD_TOOLTIP);
        }

        ImGui.sameLine();

        // Reload button
        ImGui.setNextItemWidth(44f);
        ImGui.beginDisabled(this.reloadFuture != null && !this.reloadFuture.isDone());
        if (ImGui.button(("" + (char) 0xF33F))) {
            this.reloadFuture = Minecraft.getInstance().reloadResourcePacks();
        }
        ImGui.endDisabled();
        if (ImGui.isItemHovered()) {
            VeilImGuiUtil.setTooltip(RELOAD_TOOLTIP);
        }

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
                        VeilResourceInfo info = resource.resourceInfo();
                        if (!info.hidden() && info.location().toString().contains(this.searchText.get())) {
                            resources.add(resource);
                        }
                    }

                    folders.addAll(folder.getSubFolders());
                }

                ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 4f, 4f);
                ImGuiListClipper.forEach(resources.size(), new ImListClipperCallback() {
                    @Override
                    public void accept(int index) {
                        VeilResource<?> resource = resources.get(index);

                        float startX = ImGui.getCursorScreenPosX();
                        ImGui.selectable("##" + resource.resourceInfo().location(), false, ImGuiSelectableFlags.AllowItemOverlap, ImGui.getContentRegionAvailX(), 22f);

                        ImGui.setItemAllowOverlap();
                        ImGui.sameLine();
                        ImGui.setCursorScreenPos(startX, ImGui.getCursorScreenPosY());
                        VeilResourceRenderer.renderFilename(resource, true);
                    }
                });
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

    private int renderFolderContents(VeilResourceFolder folder) {
        int count = 0;

        ImVec2 cursorScreenPos = ImGui.getCursorScreenPos();

        for (VeilResourceFolder subFolder : folder.getSubFolders()) {
            count += this.renderFolder(subFolder);
        }

        ImGui.indent();

        List<VeilResource<?>> resources = new ArrayList<>(folder.getRenderResources());
        ImGuiListClipper.forEach(resources.size(), new ImListClipperCallback() {
            @Override
            public void accept(int index) {
                VeilResource<?> resource = resources.get(index);

                float startX = ImGui.getCursorScreenPosX();
                ImGui.selectable("##" + resource.resourceInfo().location(), false, ImGuiSelectableFlags.AllowItemOverlap, ImGui.getContentRegionAvailX(), cellHeight);

                ImGui.setItemAllowOverlap();
                ImGui.sameLine();

                ImVec2 selectableCursorScreenPos = ImGui.getCursorScreenPos();

                float shift = (cellHeight - ImGui.getTextLineHeight()) / 2.0f;
                ImGui.setCursorScreenPos(startX, selectableCursorScreenPos.y + shift);
                VeilResourceRenderer.renderFilename(resource, false);
                ImGui.setCursorScreenPos(startX, ImGui.getCursorScreenPosY() - shift);
            }
        });
        count += resources.size();

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
