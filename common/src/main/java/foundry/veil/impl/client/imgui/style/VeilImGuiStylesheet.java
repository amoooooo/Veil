package foundry.veil.impl.client.imgui.style;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiDir;

public class VeilImGuiStylesheet {

    public static void initStyles() {
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowBorderSize(0);
        style.setFrameBorderSize(0);
        style.setWindowRounding(1);
        style.setWindowPadding(11, 11);
        style.setDisplaySafeAreaPadding(0, 0);
        style.setWindowTitleAlign(0.005f, 0.5f);
        style.setWindowMenuButtonPosition(ImGuiDir.Right);

        int neutral = ImColor.rgba("#3C4E6887");
        style.setColor(ImGuiCol.FrameBg, ImColor.rgba("#18223087"));
        style.setColor(ImGuiCol.Tab, neutral);
        style.setColor(ImGuiCol.CheckMark, neutral);
        style.setColor(ImGuiCol.SliderGrab, neutral);

        style.setColor(ImGuiCol.DockingPreview, neutral);

        style.setColor(ImGuiCol.MenuBarBg, ImColor.rgba("#18223087"));

        int active = ImColor.rgba("#324259FF");
        style.setColor(ImGuiCol.TitleBgActive, active);
        style.setColor(ImGuiCol.TitleBgActive, active);
        style.setColor(ImGuiCol.DragDropTarget, active);
    }

}
