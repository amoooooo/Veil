package foundry.veil.impl.client.render.wrapper;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboAttachment;
import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import foundry.veil.ext.RenderTargetExtension;
import foundry.veil.mixin.accessor.RenderTargetAccessor;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL30C.*;

/**
 * Wraps any render target with an {@link AdvancedFbo}.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class VanillaAdvancedFboWrapper implements AdvancedFbo {

    private final Supplier<RenderTarget> renderTargetSupplier;
    private final Supplier<AttachmentWrapper> colorBuffer;
    private final Supplier<AttachmentWrapper> depthBuffer;

    public VanillaAdvancedFboWrapper(Supplier<RenderTarget> renderTargetSupplier) {
        this.renderTargetSupplier = renderTargetSupplier;
        this.colorBuffer = Suppliers.memoize(() -> new AttachmentWrapper(this, () -> this.toRenderTarget().getColorTextureId(), GL_COLOR_ATTACHMENT0));
        this.depthBuffer = Suppliers.memoize(() -> new AttachmentWrapper(this, () -> this.toRenderTarget().getDepthTextureId(), GL_DEPTH_ATTACHMENT));
    }

    @Override
    public void create() {
        throw new UnsupportedOperationException("Vanilla framebuffers cannot be created");
    }

    @Override
    public void clear() {
        RenderSystem.assertOnRenderThreadOrInit();
        RenderTarget renderTarget = this.toRenderTarget();

        float[] clearChannels = ((RenderTargetAccessor) renderTarget).getClearChannels();
        RenderSystem.clearColor(clearChannels[0], clearChannels[1], clearChannels[2], clearChannels[3]);
        int mask = GL_COLOR_BUFFER_BIT;
        if (renderTarget.useDepth) {
            RenderSystem.clearDepth(1.0);
            mask |= GL_DEPTH_BUFFER_BIT;
        }

        RenderSystem.clear(mask, Minecraft.ON_OSX);
    }

    @Override
    public void bind(boolean setViewport) {
        this.toRenderTarget().bindWrite(setViewport);
    }

    @Override
    public void bindRead() {
        RenderSystem.assertOnRenderThreadOrInit();
        ((RenderTargetExtension) this.toRenderTarget()).veil$bindReadFramebuffer();
    }

    @Override
    public void bindDraw(boolean setViewport) {
        RenderSystem.assertOnRenderThreadOrInit();
        RenderTarget renderTarget = this.toRenderTarget();

        ((RenderTargetExtension) renderTarget).veil$bindDrawFramebuffer();
        if (setViewport) {
            RenderSystem.viewport(0, 0, renderTarget.viewWidth, renderTarget.viewHeight);
        }
    }

    @Override
    public int getId() {
        return this.toRenderTarget().frameBufferId;
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
        return new int[]{GL_COLOR_ATTACHMENT0};
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
            super(type, 0, 0, 0, 0, 0, 0, false, 0, 0, null);
            this.parent = parent;
            this.id = id;
        }

        @Override
        public void create() {
            throw new UnsupportedOperationException("Vanilla framebuffer attachments cannot be created");
        }

        @Override
        public void attach(int attachment) {
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
        public void free() {
            throw new UnsupportedOperationException("Vanilla framebuffer attachments cannot be deleted");
        }
    }
}
