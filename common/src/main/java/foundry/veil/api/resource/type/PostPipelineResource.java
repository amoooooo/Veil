package foundry.veil.api.resource.type;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.impl.resource.action.TextEditAction;
import imgui.extension.texteditor.TextEditorLanguageDefinition;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.InactiveProfiler;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record PostPipelineResource(VeilResourceInfo resourceInfo) implements VeilTextResource<PostPipelineResource> {

    @Override
    public List<VeilResourceAction<PostPipelineResource>> getActions() {
        return List.of(new TextEditAction<>());
    }

    @Override
    public boolean canHotReload() {
        return true;
    }

    @Override
    public void hotReload(VeilResourceManager resourceManager) throws IOException {
        VeilRenderSystem.renderer().getPostProcessingManager().reload(CompletableFuture::completedFuture, resourceManager.resources(this.resourceInfo), InactiveProfiler.INSTANCE, InactiveProfiler.INSTANCE, Util.backgroundExecutor(), Minecraft.getInstance());
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
