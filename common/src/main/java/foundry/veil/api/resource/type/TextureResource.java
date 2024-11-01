package foundry.veil.api.resource.type;

import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import net.minecraft.client.Minecraft;

import java.util.List;

public record TextureResource(VeilResourceInfo resourceInfo) implements VeilResource<TextureResource> {

    @Override
    public void render(boolean dragging) {
        float size = ImGui.getTextLineHeight();
        int texture = Minecraft.getInstance().getTextureManager().getTexture(this.resourceInfo.location()).getId();

        ImGui.pushStyleColor(ImGuiCol.Text, this.resourceInfo.isStatic() ? 0xFFAAAAAA : 0xFFFFFFFF);
        if (dragging) {
            ImGui.image(texture, size * 8, size * 8);
            VeilImGuiUtil.resourceLocation(this.resourceInfo().location());
        } else {
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
            ImGui.setItemAllowOverlap();
            ImGui.image(texture, size, size);
            ImGui.sameLine();
            ImGui.popStyleVar();

            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.image(texture, size * 16, size * 16);
                ImGui.endTooltip();
            }
            ImGui.sameLine();
            ImGui.text(this.resourceInfo.fileName());
        }
        ImGui.popStyleColor();
    }

    @Override
    public List<VeilResourceAction<TextureResource>> getActions() {
        return List.of();
    }

    @Override
    public boolean canHotReload() {
        return true;
    }

    @Override
    public void hotReload() {
    }

    @Override
    public int getIconCode() {
        return 0xF3C5; // Image file icon
    }
}
