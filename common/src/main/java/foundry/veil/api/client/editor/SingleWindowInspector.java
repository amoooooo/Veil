package foundry.veil.api.client.editor;

import foundry.veil.api.client.render.VeilRenderSystem;
import imgui.ImGui;
import imgui.type.ImBoolean;
import net.minecraft.network.chat.Component;

/**
 * Displays a single window as the editor. Automatically handles the close widget in the corner.
 *
 * @author Ocelot
 */
public abstract class SingleWindowInspector implements Inspector {

    protected final ImBoolean open = new ImBoolean();

    @Override
    public void render() {
        if (ImGui.begin(this.getWindowTitle().getString(), this.open)) {
            this.renderComponents();
        }
        ImGui.end();

        if (!this.open.get()) {
            VeilRenderSystem.renderer().getEditorManager().hide(this);
        }
    }

    @Override
    public void onShow() {
        this.open.set(true);
    }

    /**
     * Adds all components inside the window.
     */
    protected abstract void renderComponents();

    /**
     * @return The title of the window
     */
    protected Component getWindowTitle() {
        return this.getDisplayName();
    }
}
