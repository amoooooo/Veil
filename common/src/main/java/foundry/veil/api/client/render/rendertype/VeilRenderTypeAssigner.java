package foundry.veil.api.client.render.rendertype;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class VeilRenderTypeAssigner {
    private static final Map<Block, RenderType> BLOCK = new HashMap<>();
    private static final Map<Fluid, RenderType> FLUID = new HashMap<>();
    private static final Map<Item, RenderType> ITEM = new HashMap<>();

    // TODO: allow assigning custom supplier to use render type based on blockstate/fluidstate/itemstack

    public static Optional<RenderType> forBlock(BlockState blockState) {
        return Optional.ofNullable(BLOCK.get(blockState.getBlock()));
    }

    public static Optional<RenderType> forFluid(FluidState fluidState) {
        return Optional.ofNullable(FLUID.get(fluidState.getType()));
    }

    public static Optional<RenderType> forItem(ItemStack itemStack) {
        return Optional.ofNullable(ITEM.get(itemStack.getItem()));
    }

    public static void assign(Block block, RenderType renderType) {
        BLOCK.put(block, renderType);
        ItemBlockRenderTypes.TYPE_BY_BLOCK.put(block, renderType);
    }

    public static void assign(Fluid fluid, RenderType renderType) {
        FLUID.put(fluid, renderType);
        ItemBlockRenderTypes.TYPE_BY_FLUID.put(fluid, renderType);
    }

    public static void assign(Item item, RenderType renderType) {
        ITEM.put(item, renderType);
    }
}
