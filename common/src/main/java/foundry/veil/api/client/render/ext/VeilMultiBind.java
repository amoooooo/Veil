package foundry.veil.api.client.render.ext;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import org.lwjgl.opengl.ARBMultiBind;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.ARBMultiBind.glBindTextures;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;

/**
 * Provides access to {@link ARBMultiBind} functionality for all platforms.
 *
 * @author Ocelot
 */
public enum VeilMultiBind {
    LEGACY {
        @Override
        public void bindTextures(int first, IntBuffer textures) {
            int activeTexture = GlStateManager._getActiveTexture();
            for (int i = 0; i < textures.limit(); i++) {
                RenderSystem.activeTexture(GL_TEXTURE0 + first + i);
                if (first + i >= 12) {
                    glBindTexture(GL_TEXTURE_2D, textures.get(i));
                } else {
                    RenderSystem.bindTexture(textures.get(i));
                }
            }
            RenderSystem.activeTexture(activeTexture);
        }

        @Override
        public void bindTextures(int first, int... textures) {
            int activeTexture = GlStateManager._getActiveTexture();
            for (int i = 0; i < textures.length; i++) {
                RenderSystem.activeTexture(GL_TEXTURE0 + first + i);
                if (first + i >= 12) {
                    glBindTexture(GL_TEXTURE_2D, textures[i]);
                } else {
                    RenderSystem.bindTexture(textures[i]);
                }
            }
            RenderSystem.activeTexture(activeTexture);
        }
    },
    SUPPORTED {
        @Override
        public void bindTextures(int first, IntBuffer textures) {
            int invalidCount = Math.min(12 - first, textures.limit());
            for (int i = first; i < invalidCount; i++) {
                GlStateManager.TEXTURES[i].binding = textures.get(i - first);
            }

            glBindTextures(first, textures);
        }

        @Override
        public void bindTextures(int first, int... textures) {
            int invalidCount = Math.min(12 - first, textures.length);
            for (int i = first; i < invalidCount; i++) {
                GlStateManager.TEXTURES[i].binding = textures[i - first];
            }

            glBindTextures(first, textures);
        }
    };

    private static VeilMultiBind multiBind;

    /**
     * Binds the specified texture ids to sequential texture units and invalidates the GLStateManager.
     *
     * @param first    The first unit to bind to
     * @param textures The textures to bind
     */
    public abstract void bindTextures(int first, IntBuffer textures);

    /**
     * Binds the specified texture ids to sequential texture units and invalidates the GLStateManager.
     *
     * @param first    The first unit to bind to
     * @param textures The textures to bind
     */
    public abstract void bindTextures(int first, int... textures);

    /**
     * @return The best implementation of multi-bind for this platform
     */
    public static VeilMultiBind get() {
        if (multiBind == null) {
            GLCapabilities caps = GL.getCapabilities();
            if (caps.OpenGL44 || caps.GL_ARB_multi_bind) {
                multiBind = SUPPORTED;
                Veil.LOGGER.info("Texture Multi-Bind supported, using core");
            } else {
                multiBind = LEGACY;
                Veil.LOGGER.info("Texture Multi-Bind unsupported, using legacy");
            }
        }
        return multiBind;
    }
}
