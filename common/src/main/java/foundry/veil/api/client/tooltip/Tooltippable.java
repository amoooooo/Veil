package foundry.veil.api.client.tooltip;

import foundry.veil.api.client.color.ColorTheme;
import foundry.veil.api.client.tooltip.anim.TooltipTimeline;
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
public interface Tooltippable {

    /**
     * Get the tooltip components from the block entity
     *
     * @return the tooltip components
     */
    List<Component> getTooltip();

    boolean isTooltipEnabled();

    CompoundTag saveTooltipData();

    void loadTooltipData(CompoundTag tag);

    /**
     * Set the tooltip components for the block entity
     *
     * @param tooltip the tooltip components to set
     */
    void setTooltip(List<Component> tooltip);

    /**
     * Add a tooltip component to the block entity
     *
     * @param tooltip
     */
    void addTooltip(Component tooltip);

    /**
     * Add a list of tooltip components to the block entity
     *
     * @param tooltip
     */
    void addTooltip(List<Component> tooltip);

    /**
     * Add a tooltip component to the block entity
     *
     * @param tooltip
     */
    void addTooltip(String tooltip);

    /**
     * Get the theme object for the tooltip from the block entity
     *
     * @return the theme object
     * @see ColorTheme
     */
    ColorTheme getTheme();

    /**
     * Set the theme object for the tooltip from the block entity
     *
     * @param theme the theme object to set
     */
    void setTheme(ColorTheme theme);

    /**
     * Set the background color of the theme
     *
     * @param color the color to set
     */
    void setBackgroundColor(int color);

    /**
     * Set the top border color of the theme
     *
     * @param color
     */
    void setTopBorderColor(int color);

    /**
     * Set the bottom border color of the theme
     *
     * @param color
     */
    void setBottomBorderColor(int color);

    /**
     * Whether the tooltip should be rendered in worldspace or not
     *
     * @return true if the tooltip should be rendered in worldspace, false if it should be rendered in screenspace
     */
    boolean getWorldspace();

    /**
     * Obtain the timeline for the tooltip
     *
     * @return the timeline
     */
    TooltipTimeline getTimeline();

    /**
     * The stack for the tooltip to take components from
     *
     * @return the stack
     */
    ItemStack getStack();

    /**
     * Increase the tooltip width
     *
     * @return the bonus width
     */
    int getTooltipWidth();

    /**
     * Increase the tooltip height
     *
     * @return the bonus height
     */
    int getTooltipHeight();

    /**
     * Get the x offset for the tooltip
     *
     * @return
     */
    int getTooltipXOffset();

    /**
     * Get the y offset for the tooltip
     *
     * @return
     */
    int getTooltipYOffset();

    /**
     * Get the items to render in the tooltip and data about them
     *
     * @return the items
     */
    List<VeilUIItemTooltipDataHolder> getItems();

}
