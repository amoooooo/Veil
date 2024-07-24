package foundry.veil.api.event;

import net.minecraft.client.renderer.RenderType;

@FunctionalInterface
public interface VeilAssignRenderLayerEvent<T> {
    void onAssignRenderLayer(Registry<T> registry);

    @FunctionalInterface
    interface Registry<T> {
        @SuppressWarnings("unchecked")
        void assignRenderLayer(RenderType renderType, T... objs);
    }
}
