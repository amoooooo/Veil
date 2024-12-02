package foundry.veil.api.resource.type;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.impl.resource.action.ModelEditAction;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record BlockModelResource(VeilResourceInfo resourceInfo) implements VeilResource<BlockModelResource> {

    @Override
    public List<VeilResourceAction<BlockModelResource>> getActions() {
        return List.of(new ModelEditAction<>());
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

        try {
            ProfilerFiller profiler = client.getProfiler();

            // TODO: Potentially look at more targeted model reload?
            CompletableFuture.allOf(modelManager.reload(CompletableFuture::completedFuture, resources, profiler, profiler, Util.backgroundExecutor(), client)).handle((u, t) -> {
                Minecraft.getInstance().tell(VeilRenderSystem::rebuildChunks);
                return null;
            });
        } catch (Exception exception) {
            Veil.LOGGER.error("Failed to load model {}", location, exception);
        }
    }

    @Override
    public int getIconCode() {
        return 0xF383;
    }

}
