package foundry.veil.impl.client.imgui;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiDir;

public class VeilImGuiStylesheet {

    public static void initStyles() {
        ImGuiStyle style = ImGui.getStyle();


        style.setColor(ImGuiCol.Text,                    0.98f, 0.98f, 0.98f, 1.00f);
        style.setColor(ImGuiCol.TextDisabled,            0.55f, 0.55f, 0.56f, 1.00f);
        style.setColor(ImGuiCol.WindowBg,                0.15f, 0.15f, 0.15f, 0.92f);
        style.setColor(ImGuiCol.ChildBg,                 0.00f, 0.00f, 0.00f, 0.00f);
        style.setColor(ImGuiCol.PopupBg,                 0.08f, 0.08f, 0.08f, 0.94f);
        style.setColor(ImGuiCol.Border,                  0.35f, 0.35f, 0.35f, 0.50f);
        style.setColor(ImGuiCol.BorderShadow,            0.12f, 0.12f, 0.13f, 0.27f);
        style.setColor(ImGuiCol.FrameBg,                 0.12f, 0.13f, 0.13f, 0.94f);
        style.setColor(ImGuiCol.FrameBgHovered,          0.15f, 0.15f, 0.15f, 0.94f);
        style.setColor(ImGuiCol.FrameBgActive,           0.31f, 0.31f, 0.31f, 0.67f);
        style.setColor(ImGuiCol.TitleBg,                 0.14f, 0.14f, 0.15f, 1.00f);
        style.setColor(ImGuiCol.TitleBgActive,           0.14f, 0.14f, 0.15f, 1.00f);
        style.setColor(ImGuiCol.TitleBgCollapsed,        0.00f, 0.00f, 0.00f, 0.51f);
        style.setColor(ImGuiCol.MenuBarBg,               0.28f, 0.29f, 0.29f, 0.53f);
        style.setColor(ImGuiCol.ScrollbarBg,             0.02f, 0.02f, 0.02f, 0.53f);
        style.setColor(ImGuiCol.ScrollbarGrab,           0.31f, 0.31f, 0.31f, 1.00f);
        style.setColor(ImGuiCol.ScrollbarGrabHovered,    0.41f, 0.41f, 0.41f, 1.00f);
        style.setColor(ImGuiCol.ScrollbarGrabActive,     0.51f, 0.51f, 0.51f, 1.00f);
        style.setColor(ImGuiCol.CheckMark,               0.31f, 0.51f, 0.21f, 1.00f);
        style.setColor(ImGuiCol.SliderGrab,              0.40f, 0.40f, 0.40f, 0.53f);
        style.setColor(ImGuiCol.SliderGrabActive,        0.31f, 0.51f, 0.21f, 1.00f);
        style.setColor(ImGuiCol.Button,                  0.28f, 0.29f, 0.29f, 1.00f);
        style.setColor(ImGuiCol.ButtonHovered,           0.27f, 0.27f, 0.27f, 1.00f);
        style.setColor(ImGuiCol.ButtonActive,            0.27f, 0.27f, 0.27f, 1.00f);
        style.setColor(ImGuiCol.Header,                  0.73f, 0.73f, 0.73f, 0.31f);
        style.setColor(ImGuiCol.HeaderHovered,           0.41f, 0.41f, 0.41f, 0.45f);
        style.setColor(ImGuiCol.HeaderActive,            0.41f, 0.41f, 0.41f, 0.45f);
        style.setColor(ImGuiCol.Separator,               0.35f, 0.36f, 0.36f, 1.00f);
        style.setColor(ImGuiCol.SeparatorHovered,        0.10f, 0.40f, 0.75f, 0.78f);
        style.setColor(ImGuiCol.SeparatorActive,         0.10f, 0.40f, 0.75f, 1.00f);
        style.setColor(ImGuiCol.ResizeGrip,              0.11f, 0.11f, 0.11f, 1.00f);
        style.setColor(ImGuiCol.ResizeGripHovered,       0.54f, 0.54f, 0.54f, 1.00f);
        style.setColor(ImGuiCol.ResizeGripActive,        0.85f, 0.85f, 0.85f, 1.00f);
        style.setColor(ImGuiCol.Tab,                     0.32f, 0.32f, 0.32f, 0.53f);
        style.setColor(ImGuiCol.TabHovered,              0.35f, 0.35f, 0.35f, 0.80f);
        style.setColor(ImGuiCol.TabActive,               0.39f, 0.39f, 0.39f, 1.00f);
        style.setColor(ImGuiCol.TabUnfocused,            0.07f, 0.10f, 0.15f, 0.97f);
        style.setColor(ImGuiCol.TabUnfocusedActive,      0.14f, 0.26f, 0.42f, 1.00f);
        style.setColor(ImGuiCol.DockingPreview,          0.24f, 0.31f, 0.41f, 0.53f);
        style.setColor(ImGuiCol.DockingEmptyBg,          0.20f, 0.20f, 0.20f, 1.00f);
        style.setColor(ImGuiCol.PlotLines,               0.61f, 0.61f, 0.61f, 1.00f);
        style.setColor(ImGuiCol.PlotLinesHovered,        1.00f, 0.43f, 0.35f, 1.00f);
        style.setColor(ImGuiCol.PlotHistogram,           0.90f, 0.70f, 0.00f, 1.00f);
        style.setColor(ImGuiCol.PlotHistogramHovered,    1.00f, 0.60f, 0.00f, 1.00f);
        style.setColor(ImGuiCol.TableHeaderBg,           0.19f, 0.19f, 0.20f, 1.00f);
        style.setColor(ImGuiCol.TableBorderStrong,       0.31f, 0.31f, 0.35f, 1.00f);
        style.setColor(ImGuiCol.TableBorderLight,        0.23f, 0.23f, 0.25f, 1.00f);
        style.setColor(ImGuiCol.TableRowBg,              0.00f, 0.00f, 0.00f, 0.00f);
        style.setColor(ImGuiCol.TableRowBgAlt,           1.00f, 1.00f, 1.00f, 0.06f);
        style.setColor(ImGuiCol.TextSelectedBg,          0.26f, 0.59f, 0.98f, 0.35f);
        style.setColor(ImGuiCol.DragDropTarget,          0.35f, 0.28f, 0.20f, 1.00f);
        style.setColor(ImGuiCol.NavHighlight,            0.76f, 0.76f, 0.76f, 1.00f);
        style.setColor(ImGuiCol.NavWindowingHighlight,   1.00f, 1.00f, 1.00f, 0.70f);
        style.setColor(ImGuiCol.NavWindowingDimBg,       0.80f, 0.80f, 0.80f, 0.20f);
        style.setColor(ImGuiCol.ModalWindowDimBg,        0.80f, 0.80f, 0.80f, 0.35f);




        style.setWindowPadding(5f, 4f);
        style.setFramePadding(10f, 5f);
        style.setCellPadding(4f, 2f);
        style.setItemSpacing(7f, 3f);
        style.setItemInnerSpacing(4f, 4f);
        style.setTouchExtraPadding(0f, 0f);
        style.setIndentSpacing(14f);
        style.setScrollbarSize(10f);
        style.setGrabMinSize(10f);

        style.setWindowBorderSize(1f);
        style.setChildBorderSize(1f);
        style.setPopupBorderSize(1f);
        style.setFrameBorderSize(1f);
        style.setTabBorderSize(1f);

        style.setWindowRounding(1f);
        style.setChildRounding(1f);
        style.setFrameRounding(1f);
        style.setPopupRounding(1f);
        style.setScrollbarRounding(1f);
        style.setGrabRounding(1f);
        style.setLogSliderDeadzone(4f);
        style.setTabRounding(1f);


        style.setWindowMenuButtonPosition(ImGuiDir.Right);
    }

}
