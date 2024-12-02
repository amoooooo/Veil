package foundry.veil.api.resource.type;

import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.impl.resource.action.TextEditAction;
import imgui.extension.texteditor.TextEditorLanguageDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public record LanguageResource(VeilResourceInfo resourceInfo) implements VeilTextResource<LanguageResource> {

    @Override
    public List<VeilResourceAction<LanguageResource>> getActions() {
        return List.of(new TextEditAction<>());
    }

    @Override
    public boolean canHotReload() {
        return true;
    }

    @Override
    public void hotReload(VeilResourceManager resourceManager) throws IOException {
        ResourceManager resources = resourceManager.resources(this.resourceInfo);
        Minecraft client = Minecraft.getInstance();
        LanguageManager langManager = client.getLanguageManager();

        langManager.onResourceManagerReload(resources);
    }

    @Override
    public int getIconCode() {
        return 0xEDCF;
    }

    @Override
    public @Nullable TextEditorLanguageDefinition languageDefinition() {
        return null;
    }

}
