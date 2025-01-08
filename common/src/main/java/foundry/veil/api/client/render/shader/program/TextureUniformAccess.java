package foundry.veil.api.client.render.shader.program;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import foundry.veil.api.client.render.shader.texture.ShaderTextureSource;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.jetbrains.annotations.Nullable;

/**
 * Provides write access to all textures in a shader program.
 *
 * @author Ocelot
 */
public interface TextureUniformAccess {

    /**
     * Applies the {@link RenderSystem} textures to <code>Sampler0</code>-<code>Sampler11</code>.
     */
    default void addRenderSystemTextures() {
        for (int i = 0; i < 12; ++i) {
            this.addSampler("Sampler" + i, RenderSystem.getShaderTexture(i));
        }
    }

    /**
     * Sets <code>DiffuseSampler0</code>-<code>DiffuseSamplerMax</code>
     * to the color buffers in the specified framebuffer.
     *
     * @param framebuffer The framebuffer to bind samplers from
     */
    default void setFramebufferSamplers(AdvancedFbo framebuffer) {
        boolean setDiffuseSampler = false;
        for (int i = 0; i < framebuffer.getColorAttachments(); i++) {
            if (!framebuffer.isColorTextureAttachment(i)) {
                continue;
            }

            AdvancedFboTextureAttachment attachment = framebuffer.getColorTextureAttachment(i);
            this.addSampler("DiffuseSampler" + i, attachment.getId());
            if (attachment.getName() != null) {
                this.addSampler(attachment.getName(), attachment.getId());
            }
            if (!setDiffuseSampler) {
                this.addSampler("DiffuseSampler", attachment.getId());
                setDiffuseSampler = true;
            }
        }

        if (framebuffer.isDepthTextureAttachment()) {
            AdvancedFboTextureAttachment attachment = framebuffer.getDepthTextureAttachment();
            this.addSampler("DiffuseDepthSampler", attachment.getId());
            if (attachment.getName() != null) {
                this.addSampler(attachment.getName(), attachment.getId());
            }
        }
    }

    /**
     * Adds a listener for sampler updates.
     *
     * @param listener The listener instance
     */
    void addSamplerListener(SamplerListener listener);

    /**
     * Removes a listener from sampler updates.
     *
     * @param listener The listener instance
     */
    void removeSamplerListener(SamplerListener listener);

    /**
     * Adds a texture that is dynamically bound and sets texture units.
     *
     * @param name      The name of the texture to set
     * @param textureId The id of the texture to bind and assign a texture unit
     */
    void addSampler(CharSequence name, int textureId);

    /**
     * Removes the specified sampler binding.
     *
     * @param name The name of the sampler to remove
     */
    void removeSampler(CharSequence name);

    /**
     * Loads the samplers set by {@link #addSampler(CharSequence, int)} into the shader.
     *
     * @param samplerStart The sampler to start binding to
     */
    default void applyShaderSamplers(int samplerStart) {
        this.applyShaderSamplers(ShaderTextureSource.GLOBAL_CONTEXT, samplerStart);
    }

    /**
     * Loads the samplers set by {@link #addSampler(CharSequence, int)} into the shader.
     *
     * @param context The context for setting built-in shader samplers or <code>null</code> to ignore normal samplers
     * @param samplerStart The sampler to start binding to
     */
    void applyShaderSamplers(@Nullable ShaderTextureSource.Context context, int samplerStart);

    /**
     * Clears all samplers.
     */
    void clearSamplers();

    /**
     * Fired when samplers are resolved to capture the current bindings.
     */
    @FunctionalInterface
    interface SamplerListener {

        /**
         * Called to update the listener with the new texture units for the specified textures.
         *
         * @param boundSamplers A view of the textures bound
         */
        void onUpdateSamplers(Object2IntMap<CharSequence> boundSamplers);
    }
}
