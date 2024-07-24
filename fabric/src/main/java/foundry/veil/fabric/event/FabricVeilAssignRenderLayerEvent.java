package foundry.veil.fabric.event;

import foundry.veil.api.event.VeilAssignRenderLayerEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public interface FabricVeilAssignRenderLayerEvent<T> extends VeilAssignRenderLayerEvent<T> {
    Event<VeilAssignRenderLayerEvent<Block>> BLOCK = EventFactory.createArrayBacked(VeilAssignRenderLayerEvent.class, events -> registry -> {
        for (VeilAssignRenderLayerEvent<Block> event : events) {
            event.onAssignRenderLayer(registry);
        }
    });
    Event<VeilAssignRenderLayerEvent<Fluid>> FLUID = EventFactory.createArrayBacked(VeilAssignRenderLayerEvent.class, events -> registry -> {
        for (VeilAssignRenderLayerEvent<Fluid> event : events) {
            event.onAssignRenderLayer(registry);
        }
    });
    Event<VeilAssignRenderLayerEvent<Item>> ITEM = EventFactory.createArrayBacked(VeilAssignRenderLayerEvent.class, events -> registry -> {
        for (VeilAssignRenderLayerEvent<Item> event : events) {
            event.onAssignRenderLayer(registry);
        }
    });
}
