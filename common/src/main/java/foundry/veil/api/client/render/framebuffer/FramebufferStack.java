package foundry.veil.api.client.render.framebuffer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4i;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;

/**
 * Pushes/pops the current state of the bound framebuffer.
 * This enables restoring an older render state without having to assume the main framebuffer should be bound.
 *
 * @author Ocelot
 */
public class FramebufferStack {

    private static final List<State> STATE_STACK = new ArrayList<>(1);
    private static ResourceLocation lastPop;

    /**
     * Pushes the current framebuffer to the stack.
     *
     * @param name The name of the buffer to pop
     */
    public static void push(@Nullable ResourceLocation name) {
        // Make sure this isn't called multiple times
        if (name != null && !STATE_STACK.isEmpty() && name.equals(STATE_STACK.getLast().name)) {
            return;
        }

        Vector4i viewport = new Vector4i();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer bufer = stack.mallocInt(4);
            glGetIntegerv(GL_VIEWPORT, bufer);
            viewport.set(bufer);
        }

        STATE_STACK.add(new State(
                viewport,
                glGetInteger(GL_FRAMEBUFFER_BINDING),
                glGetInteger(GL_READ_FRAMEBUFFER_BINDING),
                glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING),
                name));
        lastPop = null;
    }

    /**
     * Pushes the current framebuffer and restores the previous state. If the stack is empty, the main framebuffer is bound.
     *
     * @param name The name of the buffer to pop
     */
    public static void pop(@Nullable ResourceLocation name) {
        // Make sure this isn't called multiple times in a row
        if (lastPop != null && lastPop.equals(name)) {
            return;
        }

        if (STATE_STACK.isEmpty()) {
            Veil.LOGGER.error("Popped empty Framebuffer stack");
            lastPop = null;
            return;
        }

        lastPop = name;
        State state = STATE_STACK.removeFirst();
        if (state.framebuffer == AdvancedFbo.getMainFramebuffer().getId()) {
            AdvancedFbo.unbind();
            return;
        }

        Vector4i viewport = state.viewport;
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, state.framebuffer);
        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, state.readFramebuffer);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, state.drawFramebuffer);
        RenderSystem.viewport(viewport.x, viewport.y, viewport.z, viewport.w);
    }

    /**
     * Clears the entire framebuffer stack.
     */
    public static void clear() {
        if (!STATE_STACK.isEmpty()) {
            STATE_STACK.clear();
            AdvancedFbo.unbind();
            lastPop = null;
        }
    }

    /**
     * @return Whether there are any framebuffers on the stack
     */
    public static boolean isEmpty() {
        return STATE_STACK.isEmpty();
    }

    private record State(
            Vector4i viewport,
            int framebuffer,
            int readFramebuffer,
            int drawFramebuffer,
            @Nullable ResourceLocation name) {
    }
}
