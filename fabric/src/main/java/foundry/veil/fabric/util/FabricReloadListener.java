package foundry.veil.fabric.util;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public record FabricReloadListener(ResourceLocation id,
                                   PreparableReloadListener listener) implements IdentifiableResourceReloadListener {

    @Override
    public ResourceLocation getFabricId() {
        return this.id;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return this.listener.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
    }

    @Override
    public int hashCode() {
        return this.listener.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        FabricReloadListener that = (FabricReloadListener) o;

        if (!this.id.equals(that.id)) {
            return false;
        }
        return this.listener.equals(that.listener);
    }

    @Override
    public String toString() {
        return this.listener.toString();
    }

    @Override
    public String getName() {
        return this.listener.getName();
    }
}
