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

    /**
     * Unbinds the sampler from the specified unit.
     *
     * @param unit The unit to unbind from
     */
    public static void unbind(int unit) {
        glBindSampler(unit, 0);
    }

    /**
     * @return The OpenGL id of this sampler
     */
    public int getId() {
        return this.id;
    }

    /**
     * Sets the texture filtering to match the specified state.
     *
     * @param filter The new texture filtering state to use
     */
    public void setFilter(TextureFilter filter) {
        glSamplerParameteri(this.id, GL_TEXTURE_MIN_FILTER, filter.minFilter());
        glSamplerParameteri(this.id, GL_TEXTURE_MAG_FILTER, filter.magFilter());
        this.setAnisotropy(filter.anisotropy());
        this.setCompareFunc(filter.compareFunction());
        this.setWrap(filter.wrapX(), filter.wrapY(), filter.wrapZ());
        switch (filter.borderType()) {
            case FLOAT -> this.setBorderColor(filter.borderColor());
            case INT -> this.setBorderColorI(filter.borderColor());
            case UINT -> this.setBorderColorUI(filter.borderColor());
        }
        this.setCubeMapSeamless(filter.seamless());
    }

    /**
     * Sets the minification and magnification filters to match the specified blur and mipmap states.
     *
     * @param blur   Whether to use linear or nearest neighbor filtering
     * @param mipmap Whether to interpolate between mipmap levels or not
     */
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

    /**
     * Sets the anisotropic filtering value.
     * Any value >1 is considered to be enabled.
     * Set to {@link Float#MAX_VALUE} to set to the platform maximum
     *
     * @param value The new anisotropic filtering value
     */
    public void setAnisotropy(float value) {
        if (VeilRenderSystem.textureAnisotropySupported()) {
            glSamplerParameterf(this.id, GL_TEXTURE_MAX_ANISOTROPY, Math.min(value, VeilRenderSystem.maxTextureAnisotropy()));
        }
    }

    /**
     * Sets the depth compare function for depth texture sampling.
     *
     * @param compareFunction The new function or <code>null</code> to disable
     */
    public void setCompareFunc(@Nullable TextureFilter.CompareFunction compareFunction) {
        if (compareFunction != null) {
            glSamplerParameteri(this.id, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
            glSamplerParameteri(this.id, GL_TEXTURE_COMPARE_FUNC, compareFunction.getId());
        } else {
            glSamplerParameteri(this.id, GL_TEXTURE_COMPARE_MODE, GL_NONE);
        }
    }

    /**
     * Sets the X texture wrap function.
     *
     * @param wrap The new X wrap value
     */
    public void setWrapX(TextureFilter.Wrap wrap) {
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_S, wrap.getId());
    }

    /**
     * Sets the Y texture wrap function.
     *
     * @param wrap The new Y wrap value
     */
    public void setWrapY(TextureFilter.Wrap wrap) {
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_T, wrap.getId());
    }

    /**
     * Sets the Z texture wrap function.
     *
     * @param wrap The new Z wrap value
     */
    public void setWrapZ(TextureFilter.Wrap wrap) {
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_R, wrap.getId());
    }

    /**
     * Sets the texture wrap function for all axes.
     *
     * @param wrapX The new X wrap value
     * @param wrapY The new Y wrap value
     * @param wrapZ The new Z wrap value
     */
    public void setWrap(TextureFilter.Wrap wrapX, TextureFilter.Wrap wrapY, TextureFilter.Wrap wrapZ) {
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_S, wrapX.getId());
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_T, wrapY.getId());
        glSamplerParameteri(this.id, GL_TEXTURE_WRAP_R, wrapZ.getId());
    }

    /**
     * Sets the border color to use when the wrap mode is {@link TextureFilter.Wrap#CLAMP_TO_BORDER}.
     *
     * @param red   The red value from <code>0</code> to <code>1</code>
     * @param green The green value from <code>0</code> to <code>1</code>
     * @param blue  The blue value from <code>0</code> to <code>1</code>
     * @param alpha The alpha value from <code>0</code> to <code>1</code>
     */
    public void setBorderColor(float red, float green, float blue, float alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glSamplerParameterfv(this.id, GL_TEXTURE_BORDER_COLOR, stack.floats(red, green, blue, alpha));
        }
    }

    /**
     * Sets the border color to use when the wrap mode is {@link TextureFilter.Wrap#CLAMP_TO_BORDER}.
     *
     * @param red   The red value from <code>0</code> to <code>255</code>
     * @param green The green value from <code>0</code> to <code>255</code>
     * @param blue  The blue value from <code>0</code> to <code>255</code>
     * @param alpha The alpha value from <code>0</code> to <code>255</code>
     */
    public void setBorderColor(int red, int green, int blue, int alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glSamplerParameteriv(this.id, GL_TEXTURE_BORDER_COLOR, stack.ints(red, green, blue, alpha));
        }
    }

    /**
     * Sets the border color to use when the wrap mode is {@link TextureFilter.Wrap#CLAMP_TO_BORDER}.
     *
     * @param color The color value encoded as an RGBA int
     */
    public void setBorderColor(int color) {
        this.setBorderColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
    }

    /**
     * Sets the border color to use when the wrap mode is {@link TextureFilter.Wrap#CLAMP_TO_BORDER}.
     *
     * @param red   The red value from <code>0</code> to <code>255</code>
     * @param green The green value from <code>0</code> to <code>255</code>
     * @param blue  The blue value from <code>0</code> to <code>255</code>
     * @param alpha The alpha value from <code>0</code> to <code>255</code>
     */
    public void setBorderColorI(int red, int green, int blue, int alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glGetSamplerParameterIiv(this.id, GL_TEXTURE_BORDER_COLOR, stack.ints(red, green, blue, alpha));
        }
    }

    /**
     * Sets the border color to use when the wrap mode is {@link TextureFilter.Wrap#CLAMP_TO_BORDER}.
     *
     * @param color The color value encoded as an RGBA int
     */
    public void setBorderColorI(int color) {
        this.setBorderColorI((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
    }

    /**
     * Sets the border color to use when the wrap mode is {@link TextureFilter.Wrap#CLAMP_TO_BORDER}.
     *
     * @param red   The red value from <code>0</code> to <code>255</code>
     * @param green The green value from <code>0</code> to <code>255</code>
     * @param blue  The blue value from <code>0</code> to <code>255</code>
     * @param alpha The alpha value from <code>0</code> to <code>255</code>
     */
    public void setBorderColorUI(int red, int green, int blue, int alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glGetSamplerParameterIuiv(this.id, GL_TEXTURE_BORDER_COLOR, stack.ints(red, green, blue, alpha));
        }
    }

    /**
     * Sets the border color to use when the wrap mode is {@link TextureFilter.Wrap#CLAMP_TO_BORDER}.
     *
     * @param color The color value encoded as an RGBA int
     */
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
