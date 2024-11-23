package foundry.veil.impl.client.editor;

import foundry.veil.api.client.editor.EditorAttributeProvider;
import foundry.veil.api.client.editor.SingleWindowEditor;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.deferred.light.Light;
import foundry.veil.api.client.render.deferred.light.renderer.LightRenderer;
import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.flag.ImGuiHoveredFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LightEditor extends SingleWindowEditor {

    public static final Component TITLE = Component.translatable("editor.veil.deferred_light.title");

    private static final Component ADD = Component.translatable("editor.veil.deferred_light.button.add");
    private static final Component REMOVE = Component.translatable("editor.veil.deferred_light.button.remove");
    private static final Component REMOVE_ALL = Component.translatable("editor.veil.deferred_light.button.remove_all");
    private static final Component REMOVE_ALL_DESC = Component.translatable("editor.veil.deferred_light.button.remove_all.desc");
    private static final Component SET_POSITION = Component.translatable("editor.veil.deferred_light.button.set_position");
    private static final Component ATTRIBUTES = Component.translatable("editor.veil.deferred_light.attributes");

    private final List<ResourceKey<LightTypeRegistry.LightType<?>>> lightTypes = new ArrayList<>();
    private ResourceKey<LightTypeRegistry.LightType<?>> selectedTab;

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public @Nullable Component getGroup() {
        return RENDERER_GROUP;
    }

    @Override
    public boolean isEnabled() {
        return VeilRenderSystem.renderer().getDeferredRenderer().isEnabled() && Minecraft.getInstance().level != null && LightTypeRegistry.REGISTRY.size() > 0;
    }

    @Override
    protected void renderComponents() {
        LightRenderer lightRenderer = VeilRenderSystem.renderer().getDeferredRenderer().getLightRenderer();

        if (this.selectedTab == null || !LightTypeRegistry.REGISTRY.containsKey(this.selectedTab)) {
            this.selectedTab = this.lightTypes.get(0);
        }

        LightTypeRegistry.LightType<?> lightType = LightTypeRegistry.REGISTRY.get(this.selectedTab);
        ImGui.beginDisabled(lightType == null || lightType.debugLightFactory() == null);
        if (ImGui.button(ADD.getString()) && lightType != null && lightType.debugLightFactory() != null) {
            LightTypeRegistry.DebugLightFactory factory = lightType.debugLightFactory();
            Minecraft client = Minecraft.getInstance();
            Camera mainCamera = client.gameRenderer.getMainCamera();
            lightRenderer.addLight(factory.createDebugLight(client.level, mainCamera));
        }
        ImGui.endDisabled();
        if (ImGui.isItemHovered(ImGuiHoveredFlags.None)) {
            VeilImGuiUtil.setTooltip(Component.translatable("editor.veil.deferred_light.button.add.desc", this.selectedTab.location().toString()));
        }

        ImGui.sameLine();
        ImGui.beginDisabled(lightType == null);
        if (ImGui.button(REMOVE.getString()) && lightType != null) {
            for (Light light : lightRenderer.getLights(lightType)) {
                lightRenderer.removeLight(light);
            }
        }
        ImGui.endDisabled();
        if (ImGui.isItemHovered(ImGuiHoveredFlags.None)) {
            VeilImGuiUtil.setTooltip(Component.translatable("editor.veil.deferred_light.button.remove.desc", this.selectedTab.location().toString()));
        }

        ImGui.sameLine();
        if (ImGui.button(REMOVE_ALL.getString())) {
            lightRenderer.free();
        }
        if (ImGui.isItemHovered(ImGuiHoveredFlags.None)) {
            VeilImGuiUtil.setTooltip(REMOVE_ALL_DESC);
        }

        ImGui.beginTabBar("##lights");
        for (ResourceKey<LightTypeRegistry.LightType<?>> key : this.lightTypes) {
            ResourceLocation id = key.location();
            if (ImGui.beginTabItem(id.toString())) {
                this.selectedTab = key;
                List<Light> lights = lightRenderer.getLights(LightTypeRegistry.REGISTRY.get(key));
                for (int i = 0; i < lights.size(); i++) {
                    ImGui.pushID("light" + i);
                    renderLightComponents(lights.get(i));
                    ImGui.popID();
                }
                ImGui.endTabItem();
            }
        }
        ImGui.endTabBar();
    }

    @Override
    public void onShow() {
        super.onShow();
        this.lightTypes.clear();
        this.lightTypes.addAll(LightTypeRegistry.REGISTRY.registryKeySet().stream().sorted(Comparator.comparing(ResourceKey::location)).toList());
    }

    private static void renderLightComponents(Light light) {
        ImBoolean visible = new ImBoolean(true);
        ImGui.pushID(light.hashCode());
        if (ImGui.collapsingHeader("0x%X".formatted(light.hashCode()), visible)) {
            renderLightAttributeComponents(light);
        }
        ImGui.popID();
        if (!visible.get()) {
            VeilRenderSystem.renderer().getDeferredRenderer().getLightRenderer().removeLight(light);
        }
        ImGui.separator();
    }

    private static void renderLightAttributeComponents(Light light) {
        Vector3fc lightColor = light.getColor();

        ImFloat editBrightness = new ImFloat(light.getBrightness());
        float[] editLightColor = new float[]{lightColor.x(), lightColor.y(), lightColor.z()};

        ImGui.indent();
        if (ImGui.dragScalar("brightness", ImGuiDataType.Float, editBrightness, 0.02F)) {
            light.setBrightness(editBrightness.get());
        }
        if (ImGui.colorEdit3("color", editLightColor)) {
            light.setColor(editLightColor[0], editLightColor[1], editLightColor[2]);
        }

        if (ImGui.button(SET_POSITION.getString())) {
            light.setTo(Minecraft.getInstance().gameRenderer.getMainCamera());
        }

        ImGui.newLine();
        VeilImGuiUtil.component(ATTRIBUTES);

        if (light instanceof EditorAttributeProvider editorAttributeProvider) {
            editorAttributeProvider.renderImGuiAttributes();
        }
        ImGui.unindent();
    }
}
