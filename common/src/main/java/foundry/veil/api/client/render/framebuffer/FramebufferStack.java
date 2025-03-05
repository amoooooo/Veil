package foundry.veil.api.client.render.framebuffer;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
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
     */
    public static void push() {
        Vector4i viewport = new Vector4i();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer bufer = stack.mallocInt(4);
            glGetIntegerv(GL_VIEWPORT, bufer);
            viewport.set(bufer);
        }

        STATE_STACK.add(new State(viewport, glGetInteger(GL_FRAMEBUFFER_BINDING)));
    }

    /**
     * Pushes the current framebuffer and restores the previous state. If the stack is empty, the main framebuffer is bound.
     */
    public static void pop() {
        if (STATE_STACK.isEmpty()) {
            Veil.LOGGER.error("Popped empty Framebuffer stack");
            return;
        }

        State state = STATE_STACK.removeFirst();
        if (state.framebuffer == AdvancedFbo.getMainFramebuffer().getId()) {
            AdvancedFbo.unbind();
            return;
        }

        Vector4i viewport = state.viewport;
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

    private record State(Vector4i viewport, int framebuffer) {
    }
}
