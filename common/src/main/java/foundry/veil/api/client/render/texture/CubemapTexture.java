package foundry.veil.api.client.render.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.Direction;

import static org.lwjgl.opengl.ARBDirectStateAccess.glTextureParameteri;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.*;

/**
 * {@link AbstractTexture} implementation for using a cubemap texture.
 *
 * @author Ocelot
 */
public abstract class CubemapTexture extends AbstractTexture {

    /**
     * Converts the {@link Direction} value to the correct GL cubemap enum.
     *
     * @param direction The direction to convert
     * @return The OpenGL enum
     */
    public static int getGlFace(Direction direction) {
        return switch (direction) {
            case DOWN -> GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
            case UP -> GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
            case NORTH -> GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
            case SOUTH -> GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
            case WEST -> GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
            case EAST -> GL_TEXTURE_CUBE_MAP_POSITIVE_X;
        };
    }

    @Override
    public void setFilter(boolean blur, boolean mipmap) {
        RenderSystem.assertOnRenderThreadOrInit();
        this.blur = blur;
        this.mipmap = mipmap;
        int minFilter;
        int magFilter;
        if (blur) {
            minFilter = mipmap ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR;
            magFilter = GL_LINEAR;
        } else {
            minFilter = mipmap ? GL_NEAREST_MIPMAP_LINEAR : GL_NEAREST;
            magFilter = GL_NEAREST;
        }

        if (VeilRenderSystem.directStateAccessSupported()) {
            int id = this.getId();
            glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, minFilter);
            glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, magFilter);
        } else {
            this.bind();
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, minFilter);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, magFilter);
        }
    }

    @Override
    public int getId() {
        RenderSystem.assertOnRenderThreadOrInit();
        if (this.id == -1) {
            this.id = VeilRenderSystem.createTextures(GL_TEXTURE_CUBE_MAP);
        }

        return this.id;
    }

    @Override
    public void bind() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> glBindTexture(GL_TEXTURE_CUBE_MAP, this.getId()));
        } else {
            glBindTexture(GL_TEXTURE_CUBE_MAP, this.getId());
        }
    }
}
