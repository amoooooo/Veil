package foundry.veil.impl.client.render.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.ext.VeilDebug;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboAttachment;
import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import foundry.veil.ext.RenderTargetExtension;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Locale;

import static org.lwjgl.opengl.ARBClearTexture.glClearTexImage;
import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.GL30C.*;

/**
 * Legacy implementation of {@link AdvancedFbo}.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class DSAAdvancedFboImpl extends AdvancedFboImpl {

    public DSAAdvancedFboImpl(int width, int height, AdvancedFboAttachment[] colorAttachments, @Nullable AdvancedFboAttachment depthAttachment, @Nullable String debugLabel) {
        super(width, height, colorAttachments, depthAttachment, debugLabel);
    }

    @Override
    public void create() {
        for (AdvancedFboAttachment attachment : this.colorAttachments) {
            attachment.create();
        }
        if (this.depthAttachment != null) {
            this.depthAttachment.create();
        }

        this.id = glCreateFramebuffers();

        VeilDebug debug = VeilDebug.get();
        debug.objectLabel(GL_FRAMEBUFFER, this.id, "Advanced Fbo " + this.debugLabel);

        for (int i = 0; i < this.colorAttachments.length; i++) {
            this.colorAttachments[i].attach(this, i);
        }
        if (this.depthAttachment != null) {
            this.depthAttachment.attach(this, 0);
        }

        int status = glCheckNamedFramebufferStatus(this.id, GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String error = ERRORS.containsKey(status) ? ERRORS.get(status) : "0x" + Integer.toHexString(status).toUpperCase(Locale.ROOT);
            throw new IllegalStateException("Advanced FBO status did not return GL_FRAMEBUFFER_COMPLETE. " + error);
        }

        this.currentDrawBuffers = this.drawBuffers;
        glNamedFramebufferDrawBuffers(this.id, this.drawBuffers);
    }

    @Override
    public void clear(float red, float green, float blue, float alpha, int clearMask, int... buffers) {
        if (clearMask == 0) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            boolean clearTex = VeilRenderSystem.clearTextureSupported();
            FloatBuffer color = stack.floats(red, green, blue, alpha);

            if ((clearMask & GL_COLOR_BUFFER_BIT) != 0) {
                this.drawBuffers(buffers);
                for (int buffer : buffers) {
                    int i = buffer - GL_COLOR_ATTACHMENT0;
                    if (i >= 0 && i < this.colorAttachments.length) {
                        AdvancedFboAttachment attachment = this.colorAttachments[i];
                        if (clearTex && attachment instanceof AdvancedFboTextureAttachment texture) {
                            glClearTexImage(texture.getId(), 0, GL_RGBA, GL_FLOAT, color);
                        } else {
                            glClearNamedFramebufferfv(this.id, GL_COLOR, i, color);
                        }
                    }
                }
                this.resetDrawBuffers();
            }

            if ((clearMask & GL_DEPTH_BUFFER_BIT) != 0 && this.depthAttachment != null) {
                if (clearTex && this.depthAttachment instanceof AdvancedFboTextureAttachment texture) {
                    glClearTexImage(texture.getId(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, stack.floats(1.0F));
                } else {
                    glClearNamedFramebufferfv(this.id, GL_DEPTH, 0, stack.floats(1.0F));
                }
            }
        }

        if (Minecraft.ON_OSX) {
            glGetError();
        }
    }

    @Override
    public void resetDrawBuffers() {
        if (Arrays.mismatch(this.currentDrawBuffers, this.drawBuffers) >= 0) {
            this.currentDrawBuffers = this.drawBuffers;
            glNamedFramebufferDrawBuffers(this.id, this.drawBuffers);
        }
    }

    @Override
    public void drawBuffers(int... buffers) {
        if (Arrays.mismatch(this.currentDrawBuffers, buffers) >= 0) {
            if (this.currentDrawBuffers.length != buffers.length) {
                this.currentDrawBuffers = Arrays.copyOf(buffers, buffers.length);
            } else {
                System.arraycopy(buffers, 0, this.currentDrawBuffers, 0, buffers.length);
            }
            glNamedFramebufferDrawBuffers(this.id, buffers);
        }
    }

    @Override
    public void resolveToFbo(int id, int width, int height, int mask, int filtering) {
        glBlitNamedFramebuffer(this.id, id, 0, 0, this.getWidth(), this.getHeight(), 0, 0, width, height, mask, filtering);
    }

    @Override
    public void resolveToAdvancedFbo(AdvancedFbo target, int mask, int filtering) {
        glBlitNamedFramebuffer(this.id, target.getId(), 0, 0, this.getWidth(), this.getHeight(), 0, 0, target.getWidth(), target.getHeight(), mask, filtering);
    }

    @Override
    public void resolveToRenderTarget(RenderTarget target, int mask, int filtering) {
        RenderTargetExtension extension = (RenderTargetExtension) target;
        glBlitNamedFramebuffer(this.id, extension.veil$getFramebuffer(), 0, 0, this.getWidth(), this.getHeight(), 0, 0, extension.veil$getWidth(), extension.veil$getHeight(), mask, filtering);
    }
}
