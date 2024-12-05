package foundry.veil.api.resource.type;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.impl.resource.action.ModelInspectAction;
import foundry.veil.impl.resource.action.TextEditAction;
import imgui.extension.texteditor.TextEditorLanguageDefinition;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public record BlockModelResource(VeilResourceInfo resourceInfo) implements VeilTextResource<BlockModelResource> {

    @Override
    public List<VeilResourceAction<BlockModelResource>> getActions() {
        return List.of(new TextEditAction<>(), new ModelInspectAction<>());
    }

    @Override
    public boolean canHotReload() {
        return true;
    }

    @Override
    public void hotReload(VeilResourceManager resourceManager) throws IOException {
        ResourceLocation location = this.resourceInfo.location();
        ResourceManager resources = resourceManager.resources(this.resourceInfo);
        Minecraft client = Minecraft.getInstance();
        ModelManager modelManager = client.getModelManager();
        ProfilerFiller profiler = client.getProfiler();

        // TODO: Potentially look at more targeted model reload?
        modelManager.reload(CompletableFuture::completedFuture, resources, profiler, profiler, Util.backgroundExecutor(), client)
                .thenAcceptAsync(unused -> VeilRenderSystem.rebuildChunks(), client)
                .exceptionally(e -> {
                    while (e instanceof CompletionException) {
                        e = e.getCause();
                    }
                    Veil.LOGGER.error("Failed to load model {}", location, e);
                    return null;
                });
    }

    @Override
    public int getIconCode() {
        return 0xF383;
    }

    @Override
    public @Nullable TextEditorLanguageDefinition languageDefinition() {
        return null;
    }
}
