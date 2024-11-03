package foundry.veil.api.client.editor;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.NativeResource;

/**
 * A basic panel that can be toggled in the editor view.
 *
 * @author Ocelot
 */
public interface Editor extends NativeResource {

    Component DEFAULT_GROUP = Component.translatable("editor_group.veil.default");
    Component EXAMPLE_GROUP = Component.translatable("editor_group.veil.example");
    Component DEFERRED_GROUP = Component.translatable("editor_group.veil.deferred");
    Component RENDERER_GROUP = Component.translatable("editor_group.veil.renderer");
    Component INFO_GROUP = Component.translatable("editor_group.veil.info");
    Component RESOURCE_GROUP = Component.translatable("editor_group.veil.resource");

    /**
     * Renders elements into the menu bar. Only called if {@link #isMenuBarEnabled()} is <code>true</code>.
     */
    default void renderMenuBar() {
    }

    /**
     * Renders this editor to the screen.
     */
    void render();

    /**
     * Called just before the imgui state is drawn to the screen
     */
    default void renderLast() {
    }

    /**
     * @return The visible display name of this editor
     */
    Component getDisplayName();

    /**
     * @return The name of the tab group to put this editor in
     */
    default Component getGroup() {
        return DEFAULT_GROUP;
    }

    /**
     * @return Whether this editor should be selectable.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * @return Whether this editor should draw into the menu bar
     */
    default boolean isMenuBarEnabled() {
        return false;
    }

    /**
     * Called when this editor is first opened.
     */
    default void onShow() {
    }

    /**
     * Called when this editor is no longer open.
     */
    default void onHide() {
    }

    /**
     * @return Whether this editor is open
     */
    default boolean isOpen() {
        return VeilRenderSystem.renderer().getEditorManager().isVisible(this);
    }

    /**
     * Frees any resources allocated by this editor before being destroyed.
     */
    @Override
    default void free() {
    }

    static Component group(ResourceLocation id) {
        return Component.translatable("editor_group." + id.getNamespace() + "." + id.getPath());
    }
}
