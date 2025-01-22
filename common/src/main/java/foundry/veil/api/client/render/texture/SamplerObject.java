package foundry.veil.api.client.render.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.ARBDirectStateAccess.glCreateSamplers;
import static org.lwjgl.opengl.ARBTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY;
import static org.lwjgl.opengl.GL33C.*;

/**
 * Sampler objects allow shaders to sample from the same texture storage in
 * different ways depending on what samplers are bound.
 *
 * @author Ocelot
 */
public class SamplerObject implements NativeResource {

    private final int id;

    @ApiStatus.Internal
    private SamplerObject(int id) {
        this.id = id;
    }

    /**
     * Creates a single new sampler object.
     *
     * @return A new sampler
     */
    public static SamplerObject create() {
        RenderSystem.assertOnRenderThreadOrInit();
        return new SamplerObject(VeilRenderSystem.directStateAccessSupported() ? glCreateSamplers() : glGenSamplers());
    }

    /**
     * Creates an array of sampler objects.
     *
     * @param count The number of samplers to create
     * @return An array of new sampler objects
     */
    public static SamplerObject[] create(int count) {
        SamplerObject[] fill = new SamplerObject[count];
        create(fill);
        return fill;
    }

    /**
     * Replaces each element of the specified array with a new sampler object.
     *
     * @param fill The array to fill
     */
    public static void create(SamplerObject[] fill) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (fill.length == 0) {
            return;
        }

        boolean dsa = VeilRenderSystem.directStateAccessSupported();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer samplers = stack.mallocInt(fill.length);
            if (dsa) {
                glCreateSamplers(samplers);
            } else {
                glGenSamplers(samplers);
            }

            for (int i = 0; i < samplers.limit(); i++) {
                fill[i] = new SamplerObject(samplers.get(i));
            }
        }
    }

    /**
     * Binds this sampler to the specified texture unit.
     *
     * @param unit The unit to bind to
     */
    public void bind(int unit) {
        glBindSampler(unit, this.id);
    }

    public static void unbind(int unit) {
        glBindSampler(unit, 0);
    }

    public int getId() {
        return this.id;
    }

    public void setFilter(TextureFilter filter) {
        glSamplerParameteri(this.id, GL_TEXTURE_MIN_FILTER, filter.minFilter());
        glSamplerParameteri(this.id, GL_TEXTURE_MAG_FILTER, filter.magFilter());
        this.setAnisotropy(filter.anisotropy());
        this.setCompareFunc(filter.compareFunction());
        this.setWrap(filter.wrapX(), filter.wrapY(), filter.wrapZ());
        switch (filter.edgeType()) {
            case FLOAT -> this.setBorderColor(filter.edgeColor());
            case INT -> this.setBorderColorI(filter.edgeColor());
            case UINT -> this.setBorderColorUI(filter.edgeColor());
        }
        this.setCubeMapSeamless(filter.seamless());
    }

    public void setFilter(boolean blur, boolean mipmap) {
        int minFilter;
        int magFilter;
        if (blur) {
            minFilter = mipmap ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR;
            magFilter = GL_LINEAR;
        } else {
            minFilter = mipmap ? GL_NEAREST_MIPMAP_LINEAR : GL_NEAREST;
            magFilter = GL_NEAREST;
        }

        glSamplerParameteri(this.id, GL_TEXTURE_MIN_FILTER, minFilter);
        glSamplerParameteri(this.id, GL_TEXTURE_MAG_FILTER, magFilter);
    }

    public void setAnisotropy(float value) {
        if (VeilRenderSystem.textureAnisotropySupported()) {
            glSamplerParameterf(this.id, GL_TEXTURE_MAX_ANISOTROPY, Math.min(value, VeilRenderSystem.maxTextureAnisotropy()));
        }
    }

    public void setCompareFunc(@Nullable TextureFilter.CompareFunction compareFunction) {
        if (compareFunction != null) {
            glSamplerParameteri(this.id, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
            glSamplerParameteri(this.id, GL_TEXTURE_COMPARE_FUNC, compareFunction.getId());
        } else {
            glSamplerParameteri(this.id, GL_TEXTURE_COMPARE_MODE, GL_NONE);
        }
    }

    public void setWrapX(TextureFilter.Wrap wrap) {
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_S, wrap.getId());
    }

    public void setWrapY(TextureFilter.Wrap wrap) {
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_T, wrap.getId());
    }

    public void setWrapZ(TextureFilter.Wrap wrap) {
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_R, wrap.getId());
    }

    public void setWrap(TextureFilter.Wrap wrapX, TextureFilter.Wrap wrapY, TextureFilter.Wrap wrapZ) {
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_S, wrapX.getId());
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_T, wrapY.getId());
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_R, wrapZ.getId());
    }

    public void setBorderColor(float red, float green, float blue, float alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glSamplerParameterfv(this.id, GL_TEXTURE_BORDER_COLOR, stack.floats(red, green, blue, alpha));
        }
    }

    public void setBorderColor(int red, int green, int blue, int alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glSamplerParameteriv(this.id, GL_TEXTURE_BORDER_COLOR, stack.ints(red, green, blue, alpha));
        }
    }

    public void setBorderColor(int color) {
        this.setBorderColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
    }

    public void setBorderColorI(int red, int green, int blue, int alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glGetSamplerParameterIiv(this.id, GL_TEXTURE_BORDER_COLOR, stack.ints(red, green, blue, alpha));
        }
    }

    public void setBorderColorI(int color) {
        this.setBorderColorI((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
    }

    public void setBorderColorUI(int red, int green, int blue, int alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glGetSamplerParameterIuiv(this.id, GL_TEXTURE_BORDER_COLOR, stack.ints(red, green, blue, alpha));
        }
    }

    public void setBorderColorUI(int color) {
        this.setBorderColorUI((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
    }

    /**
     * Allows filtering to work across faces of cubemaps.
     *
     * @param seamless Whether to enable seamless cube maps
     */
    public void setCubeMapSeamless(boolean seamless) {
        if (VeilRenderSystem.textureCubeMapSeamlessSupported()) {
            glSamplerParameteri(this.id, GL_TEXTURE_CUBE_MAP_SEAMLESS, seamless ? 1 : 0);
        }
    }

    @Override
    public void free() {
        glDeleteSamplers(this.id);
    }
}
