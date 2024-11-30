package foundry.veil.api.resource.type;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.FramebufferDefinition;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.impl.resource.action.TextEditAction;
import imgui.extension.texteditor.TextEditorLanguageDefinition;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public record FramebufferResource(VeilResourceInfo resourceInfo) implements VeilTextResource<FramebufferResource> {

    @Override
    public List<VeilResourceAction<FramebufferResource>> getActions() {
        return List.of(new TextEditAction<>());
    }

    @Override
    public boolean canHotReload() {
        return true;
    }

    @Override
    public void hotReload(VeilResourceManager resourceManager) throws IOException {
        try (BufferedReader reader = this.resourceInfo.openAsReader(resourceManager)) {
            DataResult<FramebufferDefinition> result = FramebufferDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader));
            if (result.error().isPresent()) {
                throw new JsonParseException(result.error().get().message());
            }
            VeilRenderSystem.renderer().getFramebufferManager().setDefinition(FramebufferManager.FRAMEBUFFER_LISTER.fileToId(this.resourceInfo.location()), result.getOrThrow());
        }
    }

    @Override
    public int getIconCode() {
        return 0xED0F;
    }

    @Override
    public @Nullable TextEditorLanguageDefinition languageDefinition() {
        return null;
    }
}
