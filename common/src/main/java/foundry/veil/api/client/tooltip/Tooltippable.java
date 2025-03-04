package foundry.veil.api.client.tooltip;

import foundry.veil.api.client.color.ColorTheme;
import foundry.veil.impl.client.render.VeilUITooltipRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Interface for components that can have a tooltip displayed when hovered over in-world
 *
 * @author amo
 * @see VeilUITooltipRenderer
 * @since 1.0.0
 */
@Deprecated
public interface Tooltippable {

    /**
     * Get the tooltip components from the block entity
     *
     * @return the tooltip components
     */
    List<Component> getTooltip();

    default boolean isTooltipEnabled() {
        return true;
    }

    default CompoundTag saveTooltipData() {
        return new CompoundTag();
    }

    default void loadTooltipData(CompoundTag tag) {
    }

    /**
     * Set the tooltip components for the block entity
     *
     * @param tooltip the tooltip components to set
     */
    default void setTooltip(List<Component> tooltip) {        
    }

    /**
     * Add a tooltip component to the block entity
     *
     * @param tooltip
     */
    default void addTooltip(Component tooltip) {
    }

    /**
     * Add a list of tooltip components to the block entity
     *
     * @param tooltip
     */
    default void addTooltip(List<Component> tooltip) {
    }

    /**
     * Add a tooltip component to the block entity
     *
     * @param tooltip
     */
    default void addTooltip(String tooltip) {
    }

    /**
     * Get the theme object for the tooltip from the block entity
     *
     * @return the theme object
     * @see ColorTheme
     */
    default ColorTheme getTheme() {
        return ColorTheme.DEFAULT;
    }

    /**
     * Set the theme object for the tooltip from the block entity
     *
     * @param theme the theme object to set
     */
    default void setTheme(ColorTheme theme) {
    }

    /**
     * Set the background color of the theme
     *
     * @param color the color to set
     */
    default void setBackgroundColor(int color) {
    }

    /**
     * Set the top border color of the theme
     *
     * @param color
     */
    default void setTopBorderColor(int color) {
    }

    /**
     * Set the bottom border color of the theme
     *
     * @param color
     */
    default void setBottomBorderColor(int color) {
    }

    /**
     * Whether the tooltip should be rendered in worldspace or not
     *
     * @return true if the tooltip should be rendered in worldspace, false if it should be rendered in screenspace
     */
    default boolean getWorldspace() {
        return false;
    }

    /**
     * The stack for the tooltip to take components from
     *
     * @return the stack
     */
    default ItemStack getStack() {
        return null;
    }

    /**
     * Increase the tooltip width
     *
     * @return the bonus width
     */
    default int getTooltipWidth() {
        return 0;
    }

    /**
     * Increase the tooltip height
     *
     * @return the bonus height
     */
    default int getTooltipHeight() {
        return 0;
    }

    /**
     * Get the x offset for the tooltip
     *
     * @return
     */
    default int getTooltipXOffset() {
        return 0;
    }

    /**
     * Get the y offset for the tooltip
     *
     * @return
     */
    default int getTooltipYOffset() {
        return 0;
    }

    /**
     * Get the items to render in the tooltip and data about them
     *
     * @return the items
     */
    default List<VeilUIItemTooltipDataHolder> getItems() {
        return List.of();
    }

}
