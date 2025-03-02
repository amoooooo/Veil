package foundry.veil.api.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A utility class to manage scissor clipping regions.
 * This allows for restricting rendering to specific rectangular areas.
 */
public final class ScissorStack {

    private final Deque<ScissorRegion> regions;

    public ScissorStack() {
        this.regions = new ArrayDeque<>();
    }

    /**
     * Pushes a new scissor clipping region onto the stack.
     * The region is automatically constrained by any existing regions on the stack.
     *
     * @param x      The x-coordinate of the top-left corner of the region.
     * @param y      The y-coordinate of the top-left corner of the region.
     * @param width  The width of the region.
     * @param height The height of the region.
     */
    public void push(int x, int y, int width, int height) {
        if (!this.regions.isEmpty()) {
            ScissorRegion parent = this.regions.peek();
            int x2 = x + width;
            x = Mth.clamp(x, parent.x, parent.x + parent.width);
            width = Mth.clamp(x2, parent.x, parent.x + parent.width) - x;
            int y2 = y + height;
            y = Mth.clamp(y, parent.y, parent.y + parent.height);
            height = Mth.clamp(y2, parent.y, parent.y + parent.height) - y;
        }

        ScissorRegion region = new ScissorRegion(x, y, width, height);
        this.regions.push(region);
        region.apply();
    }

    /**
     * Removes the top scissor clipping region from the stack.
     * If there are any regions remaining, the previous region is reapplied.
     */
    public void pop() {
        this.regions.pop();
        if (this.regions.isEmpty()) {
            RenderSystem.disableScissor();
        } else {
            this.regions.peek().apply();
        }
    }

    /**
     * Represents a single scissor clipping region.
     */
    private static class ScissorRegion {
        int x, y, width, height;

        public ScissorRegion(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        /**
         * Applies this scissor region to the rendering system.
         */
        void apply() {
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            int screenY = (int) ((Minecraft.getInstance().getWindow().getHeight() - (this.y + this.height)) * scale);
            RenderSystem.enableScissor((int) (this.x * scale), screenY, (int) (this.width * scale), (int) (this.height * scale));
        }
    }
}