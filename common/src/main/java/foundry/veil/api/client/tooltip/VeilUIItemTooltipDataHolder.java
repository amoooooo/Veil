package foundry.veil.api.client.tooltip;

import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class VeilUIItemTooltipDataHolder {

    private ItemStack itemStack;
    private Function<Float, Float> x;
    private Function<Float, Float> y;

    /**
     * Create a new VeilUIItemTooltipDataHolder. This is used to store data for items that are rendered on tooltips.
     *
     * @param itemStack the item to render
     * @param x         the x position of the item. Use the callback to modify this, you are given the current Partial Tick value.
     * @param y         the y position of the item. Use the callback to modify this, you are given the current Partial Tick value.
     */

    public VeilUIItemTooltipDataHolder(ItemStack itemStack, Function<Float, Float> x, Function<Float, Float> y) {
        this.itemStack = itemStack;
        this.x = x;
        this.y = y;
    }

    /**
     * Get the itemstack to render
     *
     * @return the itemstack
     */

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    /**
     * Get the x position of the item. Use the callback to modify this, you are given the current Partial Tick value.
     *
     * @return the x position
     */

    public Function<Float, Float> getX() {
        return this.x;
    }

    /**
     * Get the y position of the item. Use the callback to modify this, you are given the current Partial Tick value.
     *
     * @return the y position
     */

    public Function<Float, Float> getY() {
        return this.y;
    }

    /**
     * Set the itemstack to render
     *
     * @param itemStack the itemstack to set
     */

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    /**
     * Set the x position of the item. Use the callback to modify this, you are given the current Partial Tick value.
     *
     * @param x the x position to set
     */

    public void setX(Function<Float, Float> x) {
        this.x = x;
    }

    /**
     * Set the y position of the item. Use the callback to modify this, you are given the current Partial Tick value.
     *
     * @param y the y position to set
     */
    public void setY(Function<Float, Float> y) {
        this.y = y;
    }
}
