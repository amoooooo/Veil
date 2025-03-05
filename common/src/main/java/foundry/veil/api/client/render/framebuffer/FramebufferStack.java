package foundry.veil.api.client.render.framebuffer;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import net.minecraft.client.Minecraft;
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

        STATE_STACK.add(new State(viewport, glGetInteger(GL_FRAMEBUFFER_BINDING), name));
    }

    /**
     * Pushes the current framebuffer and restores the previous state. If the stack is empty, the main framebuffer is bound.
     *
     * @param name The name of the buffer to pop
     */
    public static void pop(@Nullable ResourceLocation name) {
        // Make sure this isn't called multiple times
        if (STATE_STACK.isEmpty()) {
            Veil.LOGGER.error("Popped empty Framebuffer stack");
            return;
        }

        State state = STATE_STACK.getFirst();
        if (name != null && !name.equals(state.name)) {
            return;
        }

        STATE_STACK.removeFirst();
        if (state.framebuffer == AdvancedFbo.getMainFramebuffer().getId()) {
            AdvancedFbo.unbind();
            return;
        }

        Vector4i viewport = state.viewport;
        // Macs can't handle this state change
        if (Minecraft.ON_OSX) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, state.framebuffer);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, state.framebuffer);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, state.framebuffer);
        RenderSystem.viewport(viewport.x, viewport.y, viewport.z, viewport.w);
    }

    /**
     * Clears the entire framebuffer stack.
     */
    public static void clear() {
        if (!STATE_STACK.isEmpty()) {
            STATE_STACK.clear();
            AdvancedFbo.unbind();
        }
    }

    /**
     * @return Whether there are any framebuffers on the stack
     */
    public static boolean isEmpty() {
        return STATE_STACK.isEmpty();
    }

    private record State(Vector4i viewport, int framebuffer, @Nullable ResourceLocation name) {
    }
}
