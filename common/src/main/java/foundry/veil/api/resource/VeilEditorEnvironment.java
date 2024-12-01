package foundry.veil.api.resource;

import foundry.veil.Veil;
import foundry.veil.api.client.registry.VeilResourceEditorRegistry;
import foundry.veil.api.resource.editor.ResourceFileEditor;
import net.minecraft.resources.ResourceLocation;

/**
 * An environment where files can be opened, edited, and managed.
 */
public interface VeilEditorEnvironment {

    <T extends VeilResource<?>> void open(T resource, ResourceFileEditor<T> editor);

    @SuppressWarnings({"unchecked", "rawtypes"})
    default void open(VeilResource<?> resource, ResourceLocation editorName){
        ResourceFileEditor editor = VeilResourceEditorRegistry.REGISTRY.get(editorName);
        if (editor == null) {
            Veil.LOGGER.error("Failed to find editor for resource: {}", resource.resourceInfo().location());
            return;
        }

        this.open(resource, editor);
    }

    VeilResourceManager getResourceManager();
}
