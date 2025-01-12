package foundry.veil.api.client.render.ext;

import foundry.veil.Veil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import static org.lwjgl.opengl.KHRDebug.*;

/**
 * Provides access to debug functionality for all platforms.
 *
 * @author Ocelot
 */
public enum VeilDebug {
    DISABLED {
        @Override
        public void debugMessageInsert(int type, int id, int severity, CharSequence message) {
        }

        @Override
        public void objectLabel(int identifier, int name, CharSequence label) {
        }
    },
    ENABLED {
        @Override
        public void debugMessageInsert(int type, int id, int severity, CharSequence message) {
            glDebugMessageInsert(GL_DEBUG_SOURCE_APPLICATION, type, id, severity, message);
        }

        @Override
        public void objectLabel(int identifier, int name, @Nullable CharSequence label) {
            if (label != null) {
                glObjectLabel(identifier, name, label);
            } else {
                nglObjectLabel(identifier, name, 0, 0L);
            }
        }
    };

    private static VeilDebug debug;

    /**
     * This function can be called by applications and third-party libraries to generate their own messages, such as ones containing timestamp information or signals about specific render system events.
     *
     * @param type     the type of the debug message insert
     * @param id       the user-supplied identifier of the message to insert
     * @param severity the severity of the debug messages to insert
     * @param message  a character array containing the message to insert
     */
    public abstract void debugMessageInsert(int type, int id, int severity, CharSequence message);

    /**
     * Labels a named object identified within a namespace.
     *
     * @param identifier the namespace from which the name of the object is allocated
     * @param name       the name of the object to label
     * @param label      a string containing the label to assign to the object
     */
    public abstract void objectLabel(int identifier, int name, @Nullable CharSequence label);

    /**
     * @return The best implementation of GL debug for this platform
     */
    public static VeilDebug get() {
        if (!Veil.RENDERDOC) {
            return DISABLED;
        }

        if (debug == null) {
            GLCapabilities caps = GL.getCapabilities();
            if (caps.OpenGL43 || caps.GL_KHR_debug) {
                debug = ENABLED;
                Veil.LOGGER.info("GL Debug supported");
            } else {
                debug = DISABLED;
                Veil.LOGGER.info("GL Debug unsupported");
            }
        }
        return debug;
    }
}
