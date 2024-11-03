package foundry.veil.impl.client.editor;

import foundry.veil.api.client.editor.Editor;
import foundry.veil.api.client.render.VeilRenderSystem;
import imgui.ImGui;
import imgui.type.ImBoolean;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class DemoEditor implements Editor {

    public static final Component TITLE = Component.translatable("editor.veil.example.imgui.title");

    private final ImBoolean open = new ImBoolean();

    @Override
    public void render() {
        ImGui.showDemoWindow(this.open);

        if (!this.open.get()) {
            VeilRenderSystem.renderer().getEditorManager().hide(this);
        }
    }

    @Override
    public void onShow() {
        this.open.set(true);
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public Component getGroup() {
        return EXAMPLE_GROUP;
    }
}
