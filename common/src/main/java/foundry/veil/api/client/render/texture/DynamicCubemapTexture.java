package foundry.veil.api.client.render.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.mixin.accessor.NativeImageAccessor;
import net.minecraft.core.Direction;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_LOD_BIAS;

/**
 * Dynamic implementation of {@link CubemapTexture}. Must call {@link #init(int, int)} before it can be used.
 *
 * @author Ocelot
 */
public class DynamicCubemapTexture extends CubemapTexture {

    private final int[] width;
    private final int[] height;

    public DynamicCubemapTexture() {
        this.width = new int[6];
        this.height = new int[6];
    }

    private void init(int face, int width, int height) {
        this.width[face] = width;
        this.height[face] = height;
        RenderSystem.assertOnRenderThreadOrInit();
        glTexImage2D(face, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
    }

    /**
     * Initializes each face to the same size white texture.
     *
     * @param width  The width of each face
     * @param height The height of each face
     */
    public void init(int width, int height) {
        this.bind();
        this.setFilter(false, false);
        GlStateManager._texParameter(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager._texParameter(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAX_LOD, 0);
        GlStateManager._texParameter(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_LOD_BIAS, 0.0F);

        RenderSystem.assertOnRenderThreadOrInit();
        for (int i = 0; i < 6; i++) {
            this.init(i, width, height);
        }
    }

    /**
     * Uploads the same image to all faces of the cubemap.
     *
     * @param image The image to upload
     */
    public void upload(NativeImage image) {
        this.init(image.getWidth(), image.getHeight());

        int width = image.getWidth();
        int height = image.getHeight();
        RenderSystem.assertOnRenderThreadOrInit();
        NativeImageAccessor accessor = (NativeImageAccessor) (Object) image;
        accessor.invokeCheckAllocated();
        GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
        NativeImage.Format format = image.format();
        format.setUnpackPixelStoreState();
        for (int i = 0; i < 6; i++) {
            GlStateManager._texSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, 0, 0, width, height, format.glFormat(), GL_UNSIGNED_BYTE, accessor.getPixels());
        }
    }

    /**
     * Uploads the specified image to the specified face.
     *
     * @param face  The face to upload to
     * @param image The image to upload
     */
    public void upload(Direction face, NativeImage image) {
        this.upload(getGlFace(face), image);
    }

    /**
     * Uploads the specified image to the specified face.
     *
     * @param face  The face to upload to
     * @param image The image to upload
     */
    public void upload(int face, NativeImage image) {
        if (this.id == -1) {
            this.init(image.getWidth(), image.getHeight());
        } else if (this.width[face - GL_TEXTURE_CUBE_MAP_POSITIVE_X] != image.getWidth() || this.height[face - GL_TEXTURE_CUBE_MAP_POSITIVE_X] != image.getHeight()) {
            this.init(face, image.getWidth(), image.getHeight());
        } else {
            this.bind();
        }

        int width = image.getWidth();
        int height = image.getHeight();
        RenderSystem.assertOnRenderThreadOrInit();
        NativeImageAccessor accessor = (NativeImageAccessor) (Object) image;
        accessor.invokeCheckAllocated();
        GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
        NativeImage.Format format = image.format();
        format.setUnpackPixelStoreState();
        GlStateManager._texSubImage2D(face, 0, 0, 0, width, height, format.glFormat(), GL_UNSIGNED_BYTE, accessor.getPixels());
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
    }
}
