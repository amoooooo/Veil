package foundry.veil.api.util;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author Ocelot
 */
public final class CompositeReloadListener implements PreparableReloadListener {

    private final PreparableReloadListener[] listeners;

    private CompositeReloadListener(PreparableReloadListener[] listeners) {
        this.listeners = listeners;
    }

    /**
     * Creates a reload listener that waits for all listeners to complete in stages.
     *
     * @param listeners The listeners to composite together
     * @return A new listener that properly allows listeners to use barriers
     */
    public static PreparableReloadListener of(PreparableReloadListener... listeners) {
        if (listeners.length == 0) {
            return new PreparableReloadListener() {
                @Override
                public @NotNull CompletableFuture<Void> reload(@NotNull PreparationBarrier preparationBarrier, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller prepareProfiler, @NotNull ProfilerFiller applyProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor) {
                    return preparationBarrier.wait(null);
                }

                @Override
                public @NotNull String getName() {
                    return "EmptyListener";
                }
            };
        }
        if (listeners.length == 1) {
            return listeners[0];
        }
        return new CompositeReloadListener(listeners);
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(@NotNull PreparationBarrier preparationBarrier, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller prepareProfiler, @NotNull ProfilerFiller applyProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor) {
        CompletableFuture<Unit> allComplete = new CompletableFuture<>();
        Set<PreparableReloadListener> preparingListeners = new HashSet<>(Arrays.asList(this.listeners));

        List<CompletableFuture<?>> futures = new ArrayList<>(this.listeners.length);
        for (PreparableReloadListener listener : this.listeners) {
            PreparationBarrier barrier = new PreparationBarrier() {
                @Override
                public <T> @NotNull CompletableFuture<T> wait(@Nullable T backgroundResult) {
                    if (preparingListeners.remove(listener) && preparingListeners.isEmpty()) {
                        preparationBarrier.wait(null).thenRun(() -> allComplete.complete(Unit.INSTANCE));
                    }
                    return allComplete.thenApply(unused -> backgroundResult);
                }
            };
            futures.add(listener.reload(barrier, resourceManager, prepareProfiler, applyProfiler, backgroundExecutor, gameExecutor));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public @NotNull String getName() {
        return "CompositeListener[" + Arrays.stream(this.listeners).map(PreparableReloadListener::getName).collect(Collectors.joining(",")) + "]";
    }
}
