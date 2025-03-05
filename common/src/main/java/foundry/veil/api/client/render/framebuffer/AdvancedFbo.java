package foundry.veil.api.client.render.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.texture.TextureFilter;
import foundry.veil.impl.client.render.framebuffer.AdvancedFboImpl;
import foundry.veil.impl.client.render.framebuffer.DSAAdvancedFboImpl;
import foundry.veil.impl.client.render.framebuffer.LegacyAdvancedFboImpl;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.NativeResource;

import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL30C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL30C.GL_NEAREST;
import static org.lwjgl.opengl.GL30C.GL_RGBA;

/**
 * <p>A framebuffer that has more capabilities than the vanilla Minecraft {@link RenderTarget}.</p>
 * <p>In order to resize, {@link #free()} must be called and a new framebuffer should be created.
 * Multiple color buffers of different types and depth attachments can be added.</p>
 * <p>Draw buffers are set automatically, but the default can be retrieved with {@link #getDrawBuffers()}
 * to effectively reset it. The draw buffers are set once by default and must be reset back with {@link #resetDrawBuffers()} if modified.</p>
 *
 * @author Ocelot
 * @see Builder
 */
public interface AdvancedFbo extends NativeResource {

    /**
     * Creates the framebuffer and all attachments.
     */
    void create();

    /**
     * Clears the buffers in this framebuffer.
     */
    default void clear() {
        this.clear(0.0F, 0.0F, 0.0F, 0.0F, this.getClearMask(), this.getDrawBuffers());
    }

    /**
     * Clears the specified buffers.
     *
     * @param buffers The buffers to clear
     */
    default void clear(int buffers) {
        this.clear(0.0F, 0.0F, 0.0F, 0.0F, buffers, this.getDrawBuffers());
    }

    /**
     * Clears the specified buffers.
     *
     * @param buffers      The buffers to clear
     * @param clearBuffers The color buffers to clear
     */
    default void clear(int buffers, int... clearBuffers) {
        this.clear(0.0F, 0.0F, 0.0F, 0.0F, buffers, clearBuffers);
    }

    /**
     * Clears the specified buffers.
     *
     * @param red     The red clear value
     * @param green   The green clear value
     * @param blue    The blue clear value
     * @param alpha   The alpha clear value
     * @param buffers The buffers to clear
     */
    default void clear(float red, float green, float blue, float alpha, int buffers) {
        this.clear(red, green, blue, alpha, buffers, this.getDrawBuffers());
    }

    /**
     * Clears the specified buffers.
     *
     * @param red          The red clear value
     * @param green        The green clear value
     * @param blue         The blue clear value
     * @param alpha        The alpha clear value
     * @param clearMask    The buffers to clear
     * @param clearBuffers The color buffers to clear
     */
    void clear(float red, float green, float blue, float alpha, int clearMask, int... clearBuffers);

    /**
     * Resets the draw buffers to enable all buffers.
     */
    void resetDrawBuffers();

    /**
     * Sets the buffers to draw to.
     *
     * @param buffers The texture units of the color buffers to enable
     */
    void drawBuffers(int... buffers);

    /**
     * Binds this framebuffer for read and draw requests.
     *
     * @param setViewport Whether to set the viewport to fit the bounds of this framebuffer
     */
    void bind(boolean setViewport);

    /**
     * Binds this framebuffer for read requests.
     */
    default void bindRead() {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, this.getId());
    }

    /**
     * Binds this framebuffer for draw requests.
     *
     * @param setViewport Whether to set the viewport to fit the bounds of this framebuffer
     */
    void bindDraw(boolean setViewport);

    /**
     * @return The main framebuffer
     */
    static AdvancedFbo getMainFramebuffer() {
        return AdvancedFboImpl.MAIN_WRAPPER.get();
    }

    /**
     * Binds the main Minecraft framebuffer for writing and reading.
     */
    @SuppressWarnings("ConstantValue")
    static void unbind() {
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        if (mainTarget != null) {
            mainTarget.bindWrite(true);
            return;
        }

        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0));
        } else {
            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    /**
     * Binds the main Minecraft framebuffer for reading.
     */
    static void unbindRead() {
        int mainTarget = AdvancedFbo.getMainFramebuffer().getId();
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, mainTarget));
        } else {
            GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, mainTarget);
        }
    }

    /**
     * Binds the main Minecraft framebuffer for drawing.
     */
    static void unbindDraw() {
        int mainTarget = AdvancedFbo.getMainFramebuffer().getId();
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, mainTarget));
        } else {
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, mainTarget);
        }
    }

//    /**
//     * Draws this framebuffer to the screen.
//     */
//    default void draw() {
//        RenderSystem.assertOnRenderThread();
//        Window window = Minecraft.getInstance().getWindow();
//        this.bindRead();
//        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
//        glReadBuffer(GL_BACK);
//        glBlitFramebuffer(0, 0,
//                this.getWidth(), this.getHeight(),
//                0, 0,
//                window.getWidth(), window.getHeight(),
//                GL_COLOR_BUFFER_BIT, GL_LINEAR);
//        glDrawBuffer(GL_FRONT);
//        AdvancedFbo.unbind();
//    }

    /**
     * Resolves this framebuffer to the framebuffer with the specified id as the target.
     *
     * @param id        The id of the framebuffer to copy into
     * @param width     The width of the framebuffer being copied into
     * @param height    The height of the framebuffer being copied into
     * @param mask      The buffers to copy into the provided framebuffer
     * @param filtering The filter to use if this framebuffer and the provided framebuffer are different sizes
     */
    void resolveToFbo(int id, int width, int height, int mask, int filtering);

    /**
     * Resolves this framebuffer to the provided advanced framebuffer as the target.
     *
     * @param target The target framebuffer to copy data into
     */
    default void resolveToAdvancedFbo(AdvancedFbo target) {
        this.resolveToAdvancedFbo(target,
                GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT,
                GL_NEAREST);
    }

    /**
     * Resolves this framebuffer to the provided advanced framebuffer as the target.
     *
     * @param target    The target framebuffer to copy data into
     * @param mask      The buffers to copy into the provided framebuffer
     * @param filtering The filter to use if this framebuffer and the provided framebuffer are different sizes
     */
    void resolveToAdvancedFbo(AdvancedFbo target, int mask, int filtering);

    /**
     * Resolves this framebuffer to the provided minecraft framebuffer as the target.
     *
     * @param target The target framebuffer to copy data into
     */
    default void resolveToRenderTarget(RenderTarget target) {
        this.resolveToRenderTarget(target,
                GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT,
                GL_NEAREST);
    }

    /**
     * Resolves this framebuffer to the provided minecraft framebuffer as the target.
     *
     * @param target    The target framebuffer to copy data into
     * @param mask      The buffers to copy into the provided framebuffer
     * @param filtering The filter to use if this framebuffer and the provided framebuffer are different sizes
     */
    void resolveToRenderTarget(RenderTarget target, int mask, int filtering);

    /**
     * Resolves this framebuffer to the window framebuffer as the target.
     */
    default void resolveToScreen() {
        this.resolveToAdvancedFbo(getMainFramebuffer(), GL_COLOR_BUFFER_BIT, GL_NEAREST);
    }

    /**
     * Resolves this framebuffer to the window framebuffer as the target.
     *
     * @param mask      The buffers to copy into the provided framebuffer
     * @param filtering The filter to use if this framebuffer and the provided framebuffer are different sizes
     */
    default void resolveToScreen(int mask, int filtering) {
        this.resolveToAdvancedFbo(getMainFramebuffer(), mask, filtering);
    }

    /**
     * @return The id of this framebuffer or -1 if it has been deleted
     */
    int getId();

    /**
     * @return The width of this framebuffer
     */
    int getWidth();

    /**
     * @return The height of this framebuffer
     */
    int getHeight();

    /**
     * @return The number of color attachments in this framebuffer
     */
    int getColorAttachments();

    /**
     * @return The mak used while clearing the buffer
     */
    int getClearMask();

    /**
     * @return The names of the default draw buffer state
     */
    int[] getDrawBuffers();

    /**
     * Checks to see if the provided attachment has been added to this framebuffer.
     *
     * @param attachment The attachment to check
     * @return Whether there is a valid attachment in the specified slot
     */
    boolean hasColorAttachment(int attachment);

    /**
     * @return Whether there is a depth attachment added to this framebuffer
     */
    boolean hasDepthAttachment();

    /**
     * @return Whether a depth-stencil attachment is added to this framebuffer
     */
    boolean hasStencilAttachment();

    /**
     * Checks the attachments for the specified slot.
     * If the amount of attachments is unknown, use {@link #hasColorAttachment(int)} to verify before calling this.
     *
     * @param attachment The attachment to get
     * @return The attachment in the specified attachment slot
     * @throws IllegalArgumentException If there is no attachment in the specified attachment slot
     */
    AdvancedFboAttachment getColorAttachment(int attachment);

    /**
     * Checks to see if the provided attachment has been added to this framebuffer and is a texture attachment.
     *
     * @param attachment The attachment to check
     * @return Whether there is a valid attachment in the specified slot
     */
    default boolean isColorTextureAttachment(int attachment) {
        return this.hasColorAttachment(attachment) && this.getColorAttachment(attachment) instanceof AdvancedFboTextureAttachment;
    }

    /**
     * Checks to see if the provided attachment has been added to this framebuffer and is a mutable texture attachment.
     *
     * @param attachment The attachment to check
     * @return Whether there is a valid attachment in the specified slot
     */
    default boolean isMutableColorTextureAttachment(int attachment) {
        return this.hasColorAttachment(attachment) && this.getColorAttachment(attachment) instanceof AdvancedFboMutableTextureAttachment;
    }

    /**
     * Checks to see if the provided attachment has been added to this framebuffer and is a render attachment.
     *
     * @param attachment The attachment to check
     * @return Whether there is a valid attachment in the specified slot
     */
    default boolean isColorRenderAttachment(int attachment) {
        return this.hasColorAttachment(attachment) && this.getColorAttachment(attachment) instanceof AdvancedFboRenderAttachment;
    }

    /**
     * Retrieves the attachment for the specified slot.
     * If the attachment is not known to be an {@link AdvancedFboTextureAttachment},
     * use {@link #isColorTextureAttachment(int)} before calling this.
     *
     * @param attachment The attachment to get
     * @return The texture attachment in the specified attachment slot
     * @throws IllegalArgumentException If there is no attachment in the specified attachment
     *                                  slot, or it is not an {@link AdvancedFboTextureAttachment}
     */
    default AdvancedFboTextureAttachment getColorTextureAttachment(int attachment) {
        AdvancedFboAttachment advancedFboAttachment = this.getColorAttachment(attachment);
        Validate.isTrue(this.isColorTextureAttachment(attachment), "Color attachment " + attachment + " must be a texture attachment to get texture information.");
        return (AdvancedFboTextureAttachment) advancedFboAttachment;
    }

    /**
     * Updates the 2D texture attachment reference for the specified slot.
     * If the attachment is not known to be an {@link AdvancedFboMutableTextureAttachment},
     * use {@link #isMutableColorTextureAttachment(int)} before calling this.
     *
     * @param attachment The attachment to modify
     * @param textureId  The id of the texture to draw into
     * @throws IllegalArgumentException If there is no attachment in the specified attachment
     *                                  slot, or it is not an {@link AdvancedFboMutableTextureAttachment}
     */
    default void setColorAttachmentTexture(int attachment, int textureId) {
        this.setColorAttachmentTexture(attachment, textureId, -1);
    }

    /**
     * <p>Updates the texture attachment reference for the specified slot.
     * if the attachment is not known to be an {@link AdvancedFboMutableTextureAttachment},
     * use {@link #isMutableColorTextureAttachment(int)} before calling this.</p>
     *
     * @param attachment The attachment to modify
     * @param textureId  The id of the texture to draw into
     * @param layer      The texture layer to attach. For cubemaps this is the attachment face
     * @throws IllegalArgumentException If there is no attachment in the specified attachment
     *                                  slot, or it is not an {@link AdvancedFboMutableTextureAttachment}
     */
    default void setColorAttachmentTexture(int attachment, int textureId, int layer) {
        AdvancedFboAttachment advancedFboAttachment = this.getColorAttachment(attachment);
        Validate.isTrue(this.isMutableColorTextureAttachment(attachment), "Color attachment " + attachment + " must be a mutable texture attachment to modify texture information.");
        AdvancedFboMutableTextureAttachment mutableTextureAttachment = (AdvancedFboMutableTextureAttachment) advancedFboAttachment;
        mutableTextureAttachment.setTexture(textureId, layer);
        mutableTextureAttachment.attach(this, attachment);
    }

    /**
     * Retrieves the attachment for the specified slot.
     * If the attachment is not known to be an {@link AdvancedFboRenderAttachment},
     * use {@link #isColorRenderAttachment(int)} before calling this.
     *
     * @param attachment The attachment to get
     * @return The render attachment in the specified attachment slot
     * @throws IllegalArgumentException If there is no attachment in the specified attachment
     *                                  slot, or it is not an {@link AdvancedFboRenderAttachment}
     */
    default AdvancedFboRenderAttachment getColorRenderAttachment(int attachment) {
        AdvancedFboAttachment advancedFboAttachment = this.getColorAttachment(attachment);
        Validate.isTrue(this.isColorRenderAttachment(attachment), "Color attachment " + attachment + " must be a render attachment to get render information.");
        return (AdvancedFboRenderAttachment) advancedFboAttachment;
    }

    /**
     * @return The depth attachment of this framebuffer
     * @throws IllegalArgumentException If there is no depth attachment in this framebuffer
     */
    AdvancedFboAttachment getDepthAttachment();

    /**
     * @return Whether a depth texture attachment has been added to this framebuffer
     */
    default boolean isDepthTextureAttachment() {
        return this.hasDepthAttachment() && this.getDepthAttachment() instanceof AdvancedFboTextureAttachment;
    }

    /**
     * @return Whether a mutable depth texture attachment has been added to this framebuffer
     */
    default boolean isDepthMutableTextureAttachment() {
        return this.hasDepthAttachment() && this.getDepthAttachment() instanceof AdvancedFboMutableTextureAttachment;
    }

    /**
     * @return Whether a depth render attachment has been added to this framebuffer
     */
    default boolean isDepthRenderAttachment() {
        return this.hasDepthAttachment() && this.getDepthAttachment() instanceof AdvancedFboRenderAttachment;
    }

    /**
     * Retrieves a depth buffer texture attachment.
     * If the attachment is not known to be a {@link AdvancedFboTextureAttachment},
     * use {@link #isDepthTextureAttachment()} before calling this.
     *
     * @return The texture attachment in the specified attachment slot
     * @throws IllegalArgumentException If there is no depth attachment in this framebuffer,
     *                                  or it is not an {@link AdvancedFboTextureAttachment}
     */
    default AdvancedFboTextureAttachment getDepthTextureAttachment() {
        AdvancedFboAttachment advancedFboAttachment = this.getDepthAttachment();
        Validate.isTrue(this.isDepthTextureAttachment(), "Depth attachment must be a texture attachment to get texture information.");
        return (AdvancedFboTextureAttachment) advancedFboAttachment;
    }

    /**
     * Updates the 2D depth texture attachment reference for the specified slot.
     * If the attachment is not known to be an {@link AdvancedFboMutableTextureAttachment},
     * use {@link #isMutableColorTextureAttachment(int)} before calling this.
     *
     * @param textureId The id of the texture to draw into
     * @throws IllegalArgumentException If there is no attachment in the specified attachment
     *                                  slot, or it is not an {@link AdvancedFboMutableTextureAttachment}
     */
    default void setDepthAttachmentTexture(int textureId) {
        this.setDepthAttachmentTexture(textureId, -1);
    }

    /**
     * <p>Updates the depth texture attachment reference for the specified slot.
     * If the attachment is not known to be an {@link AdvancedFboMutableTextureAttachment},
     * use {@link #isMutableColorTextureAttachment(int)} before calling this.</p>
     *
     * @param textureId The id of the texture to draw into
     * @param layer     The texture layer to attach. For cubemaps this is the attachment face
     * @throws IllegalArgumentException If there is no attachment in the specified attachment
     *                                  slot, or it is not an {@link AdvancedFboMutableTextureAttachment}
     */
    default void setDepthAttachmentTexture(int textureId, int layer) {
        AdvancedFboAttachment advancedFboAttachment = this.getDepthAttachment();
        Validate.isTrue(this.isDepthMutableTextureAttachment(), "Depth attachment must be a mutable texture attachment to modify texture information.");
        AdvancedFboMutableTextureAttachment mutableTextureAttachment = (AdvancedFboMutableTextureAttachment) advancedFboAttachment;
        mutableTextureAttachment.setTexture(textureId, layer);
        mutableTextureAttachment.attach(this, 0);
    }

    /**
     * Retrieves a depth buffer render attachment.
     * If the attachment is not known to be a {@link AdvancedFboRenderAttachment},
     * use {@link #isDepthRenderAttachment()} before calling this.
     *
     * @return The render attachment in the specified attachment slot
     * @throws IllegalArgumentException If there is no depth attachment in this framebuffer,
     *                                  or it is not an {@link AdvancedFboRenderAttachment}
     */
    default AdvancedFboRenderAttachment getDepthRenderAttachment() {
        AdvancedFboAttachment advancedFboAttachment = this.getDepthAttachment();
        Validate.isTrue(this.isDepthRenderAttachment(), "Depth attachment must be a render attachment to get render information.");
        return (AdvancedFboRenderAttachment) advancedFboAttachment;
    }

    /**
     * @return The debug label of this framebuffer in a graphics debugger
     */
    @Nullable
    String getDebugLabel();

    /**
     * @return A {@link RenderTarget} that uses this advanced fbo as the target
     */
    RenderTarget toRenderTarget();

    /**
     * Creates a new {@link AdvancedFbo} with the provided width and height.
     *
     * @param width  The width of the canvas
     * @param height The height of the canvas
     * @return A builder to construct a new FBO
     */
    static Builder withSize(int width, int height) {
        return new Builder(width, height);
    }

    /**
     * Creates a copy of the provided {@link AdvancedFbo}.
     *
     * @param parent The parent to copy attachments from
     * @return A builder to construct a new FBO
     */
    static Builder copy(AdvancedFbo parent) {
        return new Builder(parent.getWidth(), parent.getHeight()).addAttachments(parent);
    }

    /**
     * Creates a copy of the provided {@link RenderTarget}.
     *
     * @param parent The parent to copy attachments from
     * @return A builder to construct a new FBO
     */
    static Builder copy(RenderTarget parent) {
        return AdvancedFboImpl.copy(parent);
    }

    /**
     * A builder used to attach buffers to an {@link AdvancedFbo}.
     *
     * @author Ocelot
     * @see AdvancedFbo
     * @since 2.4.0
     */
    class Builder {

        private final int width;
        private final int height;
        private final List<AdvancedFboAttachment> colorAttachments;
        private AdvancedFboAttachment depthAttachment;
        private String debugLabel;

        private int levels;
        private int format;
        private int internalFormat;

        private boolean blur;
        private boolean mipmap;
        private float anisotropy;
        private TextureFilter.CompareFunction compareFunction;
        private TextureFilter.Wrap wrapS;
        private TextureFilter.Wrap wrapT;
        private int borderColor;

        private String name;

        /**
         * Creates a new builder of the specified size.
         *
         * @param width  The width of the framebuffer
         * @param height The height of the framebuffer
         */
        public Builder(int width, int height) {
            this.width = width;
            this.height = height;
            this.colorAttachments = new LinkedList<>();
            this.depthAttachment = null;
            this.debugLabel = null;
            this.reset();
        }

        private void reset() {
            this.levels = 1;
            this.format = GL_RGBA;
            this.internalFormat = GL_RGBA8;

            this.blur = false;
            this.mipmap = false;
            this.anisotropy = 1.0F;
            this.compareFunction = null;
            this.wrapS = TextureFilter.Wrap.CLAMP_TO_EDGE;
            this.wrapT = TextureFilter.Wrap.CLAMP_TO_EDGE;
            this.borderColor = 0xFF000000;

            this.name = null;
        }

        private void validateColorSize() {
            Validate.inclusiveBetween(0, VeilRenderSystem.maxColorAttachments(), this.colorAttachments.size());
        }

        private void validateSamples() {
            int samples = -1;
            for (AdvancedFboAttachment attachment : this.colorAttachments) {
                if (!(attachment instanceof AdvancedFboRenderAttachment)) {
                    continue;
                }
                if (samples == -1) {
                    samples = attachment.getLevels();
                    continue;
                }
                if (attachment.getLevels() != samples) {
                    throw new IllegalArgumentException("Framebuffer attachments need to have the same number of samples to be complete.");
                }
            }
            if (samples != -1 && this.depthAttachment instanceof AdvancedFboRenderAttachment && this.depthAttachment.getLevels() != samples) {
                throw new IllegalArgumentException("Framebuffer attachments need to have the same number of samples to be complete.");
            }
        }

        private TextureFilter.EdgeType getEdgeType() {
            if (this.format == GL_RED_INTEGER ||
                    this.format == GL_RG_INTEGER ||
                    this.format == GL_RGB_INTEGER ||
                    this.format == GL_RGBA_INTEGER) {
                return TextureFilter.EdgeType.INT;
            }
            if (this.format == GL_DEPTH_STENCIL) {
                return TextureFilter.EdgeType.UINT;
            }
            return TextureFilter.EdgeType.FLOAT;
        }

        /**
         * Adds copies of the buffers inside the specified fbo.
         *
         * @param parent The parent to add the attachments for
         */
        public Builder addAttachments(AdvancedFbo parent) {
            for (int i = 0; i < parent.getColorAttachments(); i++) {
                this.colorAttachments.add(parent.getColorAttachment(i).clone());
            }
            this.validateColorSize();
            if (parent.hasDepthAttachment()) {
                Validate.isTrue(this.depthAttachment == null, "Only one depth attachment can be applied to an FBO.");
                this.depthAttachment = parent.getDepthAttachment().clone();
            }
            return this;
        }

        /**
         * Adds copies of the buffers inside the specified fbo.
         *
         * @param parent The parent to add the attachments for
         */
        public Builder addAttachments(RenderTarget parent) {
            this.reset();
            this.addColorTextureBuffer(parent.width, parent.height, GL_UNSIGNED_BYTE);
            if (parent.useDepth) {
                Validate.isTrue(this.depthAttachment == null, "Only one depth attachment can be applied to an FBO.");
                this.setDepthRenderBuffer(parent.width, parent.height);
            }
            return this;
        }

        /**
         * Sets the number of samples to use for render buffer and texture attachments.
         * <code>1</code> is the default for single sample buffers.
         *
         * @param levels The samples to have
         */
        public Builder setLevels(int levels) {
            this.levels = levels;
            return this;
        }

        /**
         * Sets the format to use for texture attachments.
         *
         * @param format The new format to use
         */
        public Builder setFormat(FramebufferAttachmentDefinition.Format format) {
            return this.setFormat(format.getFormat(), format.getInternalFormat());
        }

        /**
         * Sets the format to use for texture attachments. {@link GL11C#GL_RGBA} is the default.
         *
         * @param format         The new format to use
         * @param internalFormat The new internal format to use
         */
        public Builder setFormat(int format, int internalFormat) {
            this.format = format;
            this.internalFormat = internalFormat;
            return this;
        }

        /**
         * Sets the sampling mode for textures.
         *
         * @param blur   Whether linear filtering should be used
         * @param mipmap Whether the framebuffer has any mipmaps that should be respected
         */
        public Builder setFilter(boolean blur, boolean mipmap) {
            this.blur = blur;
            this.mipmap = mipmap;
            return this;
        }

        /**
         * Sets the anisotropic filtering value. Set to {@link Float#MAX_VALUE} to set to the platform maximum.
         *
         * @param anisotropy The value to use. Any value >1 is considered to be enabled
         */
        public Builder setAnisotropy(float anisotropy) {
            this.anisotropy = anisotropy;
            return this;
        }

        /**
         * Sets the compare function to use. Only valid for depth textures.
         *
         * @param compareFunction The compare function to use
         */
        public Builder setCompareFunction(@Nullable TextureFilter.CompareFunction compareFunction) {
            this.compareFunction = compareFunction;
            return this;
        }

        /**
         * Sets the clamping for the X axis on the texture.
         *
         * @param wrapS The wrap mode
         */
        public Builder setWrapS(TextureFilter.Wrap wrapS) {
            this.wrapS = wrapS;
            return this;
        }

        /**
         * Sets the clamping for the X axis on the texture.
         *
         * @param wrapS The wrap mode
         */
        public Builder setWrapS(int wrapS) {
            this.wrapS = TextureFilter.Wrap.BY_GL_ID.getOrDefault(wrapS, TextureFilter.Wrap.CLAMP_TO_EDGE);
            return this;
        }

        /**
         * Sets the clamping for the Y axis on the texture.
         *
         * @param wrapT The wrap mode
         */
        public Builder setWrapT(TextureFilter.Wrap wrapT) {
            this.wrapT = wrapT;
            return this;
        }

        /**
         * Sets the clamping for the Y axis on the texture.
         *
         * @param wrapT The wrap mode
         */
        public Builder setWrapT(int wrapT) {
            this.wrapT = TextureFilter.Wrap.BY_GL_ID.getOrDefault(wrapT, TextureFilter.Wrap.CLAMP_TO_EDGE);
            return this;
        }

        /**
         * Sets the clamping for the X and Y axis on the texture.
         *
         * @param wrapS The X wrap mode
         * @param wrapT The Y wrap mode
         */
        public Builder setWrap(TextureFilter.Wrap wrapS, TextureFilter.Wrap wrapT) {
            this.wrapS = wrapS;
            this.wrapT = wrapT;
            return this;
        }

        /**
         * Sets the clamping for the X and Y axis on the texture.
         *
         * @param wrapS The X wrap mode
         * @param wrapT The Y wrap mode
         */
        public Builder setWrap(int wrapS, int wrapT) {
            this.wrapS = TextureFilter.Wrap.BY_GL_ID.getOrDefault(wrapS, TextureFilter.Wrap.CLAMP_TO_EDGE);
            this.wrapT = TextureFilter.Wrap.BY_GL_ID.getOrDefault(wrapT, TextureFilter.Wrap.CLAMP_TO_EDGE);
            return this;
        }

        /**
         * Sets the color to get when sampling the texture out of bounds when using {@link TextureFilter.Wrap#CLAMP_TO_BORDER}.
         *
         * @param borderColor The new border color in ARGB
         */
        public Builder setBorderColor(int borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        /**
         * Sets the texture filtering options to match the specified filter.
         *
         * @param filter The texture filtering options
         */
        public Builder setFilter(TextureFilter filter) {
            this.setFilter(filter.blur(), filter.mipmap());
            this.setAnisotropy(filter.anisotropy());
            this.setCompareFunction(filter.compareFunction());
            this.setWrap(filter.wrapX(), filter.wrapY());
            this.setBorderColor(filter.borderColor());
            return this;
        }

        /**
         * Sets the uniform name for textures.
         *
         * @param name The custom name to use for the sampler in shaders
         */
        public Builder setName(@Nullable String name) {
            this.name = name;
            return this;
        }

        /**
         * Adds the specified color attachment.
         *
         * @param attachment The attachment to add
         */
        public Builder addColorBuffer(AdvancedFboAttachment attachment) {
            this.colorAttachments.add(attachment);
            this.reset();
            this.validateColorSize();
            return this;
        }

        /**
         * Adds the specified texture as a texture attachment.
         *
         * @param textureId The id of the texture to add
         */
        public Builder addColorTextureWrapper(int textureId) {
            return this.addColorTextureWrapper(textureId, -1);
        }

        /**
         * Adds the specified texture as a texture attachment.
         *
         * @param textureId The id of the texture to add
         * @param layer     The layer of the texture to use
         */
        public Builder addColorTextureWrapper(int textureId, int layer) {
            return this.addColorBuffer(new AdvancedFboMutableTextureAttachment(
                    GL_COLOR_ATTACHMENT0,
                    textureId,
                    layer,
                    this.name));
        }

        /**
         * Adds a color texture buffer with the size of the framebuffer and {@link GL11C#GL_UNSIGNED_BYTE GL_UNSIGNED_BYTE} as the format.
         */
        public Builder addColorTextureBuffer() {
            return this.addColorTextureBuffer(this.width, this.height, GL_UNSIGNED_BYTE);
        }

        /**
         * Adds a color texture buffer with the specified size and {@link GL11C#GL_UNSIGNED_BYTE GL_UNSIGNED_BYTE} as the format.
         *
         * @param width  The width of the texture buffer
         * @param height The height of the texture buffer
         */
        public Builder addColorTextureBuffer(int width, int height) {
            return this.addColorTextureBuffer(width, height, GL_UNSIGNED_BYTE);
        }

        /**
         * Adds a color texture buffer with the specified data type.
         *
         * @param dataType The format of the data internally
         */
        public Builder addColorTextureBuffer(int dataType) {
            return this.addColorTextureBuffer(this.width, this.height, dataType);
        }

        /**
         * Adds a color texture buffer with the specified size and data type.
         *
         * @param width    The width of the texture buffer
         * @param height   The height of the texture buffer
         * @param dataType The format of the data internally
         */
        public Builder addColorTextureBuffer(int width, int height, int dataType) {
            Validate.isTrue(this.format != GL_DEPTH_COMPONENT && this.format != GL_DEPTH_STENCIL, "A color renderable format must be specified for color attachments");
            return this.addColorBuffer(new AdvancedFboTextureAttachment(
                    GL_COLOR_ATTACHMENT0,
                    this.internalFormat,
                    this.format,
                    dataType,
                    width,
                    height,
                    this.levels,
                    new TextureFilter(this.blur, this.mipmap, this.anisotropy, this.compareFunction, this.wrapS, this.wrapT, TextureFilter.Wrap.CLAMP_TO_EDGE, this.borderColor, this.getEdgeType(), false),
                    this.name));
        }

        /**
         * <p>Adds a color render buffer with the size of the framebuffer and 1 sample.</p>
         * <p><b><i>NOTE: COLOR RENDER BUFFERS CAN ONLY BE COPIED TO OTHER FRAMEBUFFERS</i></b></p>
         */
        public Builder addColorRenderBuffer() {
            return this.addColorRenderBuffer(this.width, this.height);
        }

        /**
         * <p>Adds a color render buffer with the specified size and the specified samples.</p>
         * <p><b><i>NOTE: COLOR RENDER BUFFERS CAN ONLY BE COPIED TO OTHER FRAMEBUFFERS</i></b></p>
         *
         * @param width  The width of the render buffer
         * @param height The height of the render buffer
         */
        public Builder addColorRenderBuffer(int width, int height) {
            Validate.isTrue(this.format != GL_DEPTH_COMPONENT && this.format != GL_DEPTH_STENCIL, "A color renderable format must be specified for color attachments");
            return this.addColorBuffer(new AdvancedFboRenderAttachment(
                    GL_COLOR_ATTACHMENT0,
                    this.internalFormat,
                    width,
                    height,
                    this.levels));
        }

        /**
         * Sets the depth buffer to the specified attachment.
         *
         * @param attachment The attachment to add
         */
        public Builder setDepthBuffer(@Nullable AdvancedFboAttachment attachment) {
            this.depthAttachment = attachment;
            this.reset();
            return this;
        }

        /**
         * Adds the specified texture as a texture attachment.
         *
         * @param textureId The id of the texture to add
         */
        public Builder setDepthTextureWrapper(int textureId) {
            return this.setDepthTextureWrapper(textureId, -1);
        }

        /**
         * Adds the specified texture as a texture attachment.
         *
         * @param textureId The id of the texture to add
         * @param layer     The layer of the texture to use
         */
        public Builder setDepthTextureWrapper(int textureId, int layer) {
            return this.setDepthBuffer(new AdvancedFboMutableTextureAttachment(
                    GL_DEPTH_ATTACHMENT,
                    textureId,
                    layer,
                    this.name));
        }

        /**
         * Sets the depth texture buffer to the size of the framebuffer and {@link GL11C#GL_FLOAT GL_FLOAT} as the format.
         */
        public Builder setDepthTextureBuffer() {
            return this.setDepthTextureBuffer(this.width, this.height, this.format == GL_DEPTH_STENCIL ? GL_UNSIGNED_INT_24_8 : GL_FLOAT);
        }

        /**
         * Sets the depth texture buffer to the specified size and {@link GL11C#GL_FLOAT GL_FLOAT} as the format.
         *
         * @param width  The width of the texture buffer
         * @param height The height of the texture buffer
         */
        public Builder setDepthTextureBuffer(int width, int height) {
            return this.setDepthTextureBuffer(width, height, this.format == GL_DEPTH_STENCIL ? GL_UNSIGNED_INT_24_8 : GL_FLOAT);
        }

        /**
         * Sets the depth texture buffer to the size of the framebuffer and specified data type.
         *
         * @param dataType The format of the data internally
         */
        public Builder setDepthTextureBuffer(int dataType) {
            return this.setDepthTextureBuffer(this.width, this.height, dataType);
        }

        /**
         * Sets the depth texture buffer to the specified size and data type.
         *
         * @param width    The width of the texture buffer
         * @param height   The height of the texture buffer
         * @param dataType The format of the data internally
         */
        public Builder setDepthTextureBuffer(int width, int height, int dataType) {
            Validate.isTrue(this.format == GL_DEPTH_COMPONENT || this.format == GL_DEPTH_STENCIL, "A depth or depth stencil format must be specified for depth attachments");
            return this.setDepthBuffer(new AdvancedFboTextureAttachment(
                    this.format == GL_DEPTH_STENCIL ? GL_DEPTH_STENCIL_ATTACHMENT : GL_DEPTH_ATTACHMENT,
                    this.internalFormat,
                    this.format,
                    dataType,
                    width,
                    height,
                    this.levels,
                    new TextureFilter(this.blur, this.mipmap, this.anisotropy, this.compareFunction, this.wrapS, this.wrapT, TextureFilter.Wrap.CLAMP_TO_EDGE, this.borderColor, this.getEdgeType(), false),
                    this.name));
        }

        /**
         * <p>Sets the depth texture buffer to the size of the framebuffer and 1 sample.</p>
         * <p><b><i>NOTE: DEPTH RENDER BUFFERS CAN ONLY BE COPIED TO OTHER FRAMEBUFFERS</i></b></p>
         */
        public Builder setDepthRenderBuffer() {
            return this.setDepthRenderBuffer(this.width, this.height);
        }

        /**
         * <p>Sets the depth texture buffer to the specified size and the specified samples.</p>
         * <p><b><i>NOTE: DEPTH RENDER BUFFERS CAN ONLY BE COPIED TO OTHER FRAMEBUFFERS</i></b></p>
         *
         * @param width  The width of the render buffer
         * @param height The height of the render buffer
         */
        public Builder setDepthRenderBuffer(int width, int height) {
            Validate.isTrue(this.format == GL_DEPTH_COMPONENT || this.format == GL_DEPTH_STENCIL, "A depth or depth stencil format must be specified for depth attachments");
            return this.setDepthBuffer(new AdvancedFboRenderAttachment(
                    this.format,
                    this.internalFormat,
                    width,
                    height,
                    this.levels));
        }

        /**
         * Sets the debug label for this framebuffer and all attachments in a graphics debugger.
         *
         * @param debugLabel The new label or <code>null</code> to use the default
         */
        public Builder setDebugLabel(@Nullable String debugLabel) {
            this.debugLabel = debugLabel;
            return this;
        }

        /**
         * Constructs a new {@link AdvancedFbo} with the specified attachments.
         *
         * @param create Whether to immediately create the buffer
         * @return A new {@link AdvancedFbo} with the specified builder properties.
         */
        public AdvancedFbo build(boolean create) {
            if (this.colorAttachments.isEmpty()) {
                throw new IllegalArgumentException("Framebuffer needs at least one color attachment to be complete.");
            }
            if (this.width <= 0 || this.height <= 0) {
                throw new IllegalArgumentException("Framebuffer needs a positive area to be complete. (Was " + this.width + "x" + this.height + ")");
            }
            this.validateSamples();
            AdvancedFbo framebuffer = VeilRenderSystem.directStateAccessSupported() ?
                    new DSAAdvancedFboImpl(this.width, this.height, this.colorAttachments.toArray(AdvancedFboAttachment[]::new), this.depthAttachment, this.debugLabel) :
                    new LegacyAdvancedFboImpl(this.width, this.height, this.colorAttachments.toArray(AdvancedFboAttachment[]::new), this.depthAttachment, this.debugLabel);
            if (create) {
                framebuffer.create();
            }
            return framebuffer;
        }
    }
}
