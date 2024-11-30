package foundry.veil.api.resource.type;

import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.impl.resource.action.TextEditAction;
import imgui.extension.texteditor.TextEditorLanguageDefinition;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public record TextResource(VeilResourceInfo resourceInfo, Type type) implements VeilTextResource<TextResource> {

    @Override
    public List<VeilResourceAction<TextResource>> getActions() {
        return List.of(new TextEditAction<>());
    }

    @Override
    public boolean canHotReload() {
        return false;
    }

    @Override
    public void hotReload(VeilResourceManager resourceManager) throws IOException {
    }

    @Override
    public int getIconCode() {
        return this.type.getIcon();
    }

    @Override
    public @Nullable TextEditorLanguageDefinition languageDefinition() {
        return null;
    }

    public enum Type {
        TEXT(".txt", 0xED0F),
        JSON(".json", 0xECCD);

        private final String extension;
        private final int icon;

        Type(String extension, int icon) {
            this.extension = extension;
            this.icon = icon;
        }

        public String getExtension() {
            return this.extension;
        }

        public int getIcon() {
            return this.icon;
        }
    }
}
