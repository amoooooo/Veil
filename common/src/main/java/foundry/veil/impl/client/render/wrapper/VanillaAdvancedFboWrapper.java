package foundry.veil.impl.client.render.wrapper;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboAttachment;
import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import foundry.veil.api.client.render.texture.TextureFilter;
import foundry.veil.ext.RenderTargetExtension;
import foundry.veil.mixin.framebuffer.client.FramebufferRenderTargetAccessor;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL30C.*;

/**
 * Wraps any render target with an {@link AdvancedFbo}.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public abstract class VanillaAdvancedFboWrapper implements AdvancedFbo {

    private final Supplier<RenderTarget> renderTargetSupplier;
    private final Supplier<AttachmentWrapper> colorBuffer;
    private final Supplier<AttachmentWrapper> depthBuffer;
    private final int[] drawBuffers;

    public VanillaAdvancedFboWrapper(Supplier<RenderTarget> renderTargetSupplier) {
        this.renderTargetSupplier = renderTargetSupplier;
        this.colorBuffer = Suppliers.memoize(() -> new AttachmentWrapper(this, () -> this.toRenderTarget().getColorTextureId(), GL_COLOR_ATTACHMENT0));
        this.depthBuffer = Suppliers.memoize(() -> new AttachmentWrapper(this, () -> this.toRenderTarget().getDepthTextureId(), GL_DEPTH_ATTACHMENT));
        this.drawBuffers = new int[]{GL_COLOR_ATTACHMENT0};
    }

    @Override
    public void create() {
        throw new UnsupportedOperationException("Vanilla framebuffers cannot be created");
    }

    @Override
    public void clear() {
        float[] clearChannels = ((FramebufferRenderTargetAccessor) this.toRenderTarget()).getClearChannels();
        this.clear(clearChannels[0], clearChannels[1], clearChannels[2], clearChannels[3], this.getClearMask(), this.getDrawBuffers());
    }

    @Override
    public void clear(int buffers) {
        float[] clearChannels = ((FramebufferRenderTargetAccessor) this.toRenderTarget()).getClearChannels();
        this.clear(clearChannels[0], clearChannels[1], clearChannels[2], clearChannels[3], buffers, this.getDrawBuffers());
    }

    // Don't do anything here because there's no point in disabling the ONLY draw buffer. Use glColorMask instead

    @Override
    public void resetDrawBuffers() {
    }

    @Override
    public void drawBuffers(int... buffers) {
    }

    @Override
    public void bind(boolean setViewport) {
        this.toRenderTarget().bindWrite(setViewport);
    }

    @Override
    public void bindDraw(boolean setViewport) {
        RenderTarget renderTarget = this.toRenderTarget();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, ((RenderTargetExtension) renderTarget).veil$getFramebuffer());
        if (setViewport) {
            RenderSystem.viewport(0, 0, renderTarget.viewWidth, renderTarget.viewHeight);
        }
    }

    @Override
    public int getId() {
        return ((RenderTargetExtension) this.toRenderTarget()).veil$getFramebuffer();
    }

    @Override
    public int getWidth() {
        return this.toRenderTarget().width;
    }

    @Override
    public int getHeight() {
        return this.toRenderTarget().height;
    }

    @Override
    public int getColorAttachments() {
        return 1;
    }

    @Override
    public int getClearMask() {
        return GL_COLOR_BUFFER_BIT | (this.toRenderTarget().useDepth ? GL_DEPTH_BUFFER_BIT : 0);
    }

    @Override
    public int[] getDrawBuffers() {
        return this.drawBuffers;
    }

    @Override
    public boolean hasColorAttachment(int attachment) {
        return attachment == 0;
    }

    @Override
    public boolean hasDepthAttachment() {
        return this.toRenderTarget().useDepth;
    }

    @Override
    public AdvancedFboAttachment getColorAttachment(int attachment) {
        Validate.isTrue(this.hasColorAttachment(attachment), "Color attachment " + attachment + " does not exist.");
        return this.colorBuffer.get();
    }

    @Override
    public AdvancedFboAttachment getDepthAttachment() {
        Validate.isTrue(this.hasDepthAttachment(), "Depth attachment does not exist.");
        return this.depthBuffer.get();
    }

    @Override
    public @Nullable String getDebugLabel() {
        return null;
    }

    @Override
    public RenderTarget toRenderTarget() {
        return this.renderTargetSupplier.get();
    }

    @Override
    public void free() {
        this.toRenderTarget().destroyBuffers();
    }

    private static class AttachmentWrapper extends AdvancedFboTextureAttachment {

        private final AdvancedFbo parent;
        private final IntSupplier id;

        private AttachmentWrapper(AdvancedFbo parent, IntSupplier id, int type) {
            super(type, 0, 0, 0, 0, 0, 0, TextureFilter.CLAMP, null);
            this.parent = parent;
            this.id = id;
        }

        @Override
        public void create() {
            throw new UnsupportedOperationException("Vanilla framebuffer attachments cannot be created");
        }

        @Override
        public void attach(AdvancedFbo framebuffer, int attachment) {
            throw new UnsupportedOperationException("Vanilla framebuffer attachments cannot be attached");
        }

        @Override
        public int getId() {
            return this.id.getAsInt();
        }

        @Override
        public @NotNull AdvancedFboTextureAttachment clone() {
            return new VanillaAdvancedFboWrapper.AttachmentWrapper(this.parent, this.id, this.getAttachmentType());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            AttachmentWrapper that = (AttachmentWrapper) o;
            return this.id.getAsInt() == that.id.getAsInt();
        }

        @Override
        public int hashCode() {
            return this.id.getAsInt();
        }

        @Override
        public void free() {
            throw new UnsupportedOperationException("Vanilla framebuffer attachments cannot be deleted");
        }
    }
}
