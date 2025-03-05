package foundry.veil.impl.client.render.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Locale;

import static org.lwjgl.opengl.ARBClearTexture.glClearTexImage;
import static org.lwjgl.opengl.GL20C.glDrawBuffers;
import static org.lwjgl.opengl.GL30C.*;

/**
 * Legacy implementation of {@link AdvancedFbo}.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class LegacyAdvancedFboImpl extends AdvancedFboImpl {

    public LegacyAdvancedFboImpl(int width, int height, AdvancedFboAttachment[] colorAttachments, @Nullable AdvancedFboAttachment depthAttachment, @Nullable String debugLabel) {
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
        RenderSystem.bindTexture(0);

        int oldFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
        this.id = glGenFramebuffers();
        this.bind(false);

        VeilDebug debug = VeilDebug.get();
        debug.objectLabel(GL_FRAMEBUFFER, this.id, "Advanced Fbo " + this.debugLabel);

        for (int i = 0; i < this.colorAttachments.length; i++) {
            this.colorAttachments[i].attach(this, i);
        }
        if (this.depthAttachment != null) {
            this.depthAttachment.attach(this, 0);
        }

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String error = ERRORS.containsKey(status) ? ERRORS.get(status) : "0x" + Integer.toHexString(status).toUpperCase(Locale.ROOT);
            throw new IllegalStateException("Advanced FBO status did not return GL_FRAMEBUFFER_COMPLETE. " + error);
        }

        this.currentDrawBuffers = this.drawBuffers;
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, oldFbo);
    }

    @Override
    public void clear(float red, float green, float blue, float alpha, int clearMask, int... buffers) {
        if (clearMask == 0) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            boolean clearTex = VeilRenderSystem.clearTextureSupported();
            FloatBuffer color = stack.floats(red, green, blue, alpha);

            int oldFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
            if (oldFbo != this.id) {
                this.bind(false);
            }

            if ((clearMask & GL_COLOR_BUFFER_BIT) != 0) {
                this.drawBuffers(buffers);
                for (int buffer : buffers) {
                    int i = buffer - GL_COLOR_ATTACHMENT0;
                    if (i >= 0 && i < this.colorAttachments.length) {
                        AdvancedFboAttachment attachment = this.colorAttachments[i];
                        if (clearTex && attachment instanceof AdvancedFboTextureAttachment texture) {
                            glClearTexImage(texture.getId(), 0, GL_RGBA, GL_FLOAT, color);
                        } else {
                            glClearBufferfv(GL_COLOR, i, color);
                        }
                    }
                }
                this.resetDrawBuffers();
            }

            if (this.depthAttachment != null) {
                boolean depth = (clearMask & GL_DEPTH_BUFFER_BIT) != 0;
                boolean stencil = this.hasStencil && (clearMask & GL_STENCIL_BUFFER_BIT) != 0;
                if (!depth && !stencil) {
                    return;
                }

                if (this.hasStencil) {
                    if (depth && stencil) {
                        if (clearTex && this.depthAttachment instanceof AdvancedFboTextureAttachment texture) {
                            glClearTexImage(texture.getId(), 0, GL_DEPTH_STENCIL, GL_FLOAT_32_UNSIGNED_INT_24_8_REV, (ByteBuffer) null);
                        } else {
                            glClearBufferfi(GL_DEPTH_STENCIL, 0, 1.0F, 0);
                        }
                    } else {
                        // Can't clear the texture if only clearing depth or stencil
                        if (depth) {
                            glClearBufferfv(GL_DEPTH, 0, stack.floats(1.0F));
                        }
                        if (stencil) {
                            glClearBufferiv(GL_STENCIL, 0, stack.ints(0));
                        }
                    }
                } else {
                    if (clearTex && this.depthAttachment instanceof AdvancedFboTextureAttachment texture) {
                        glClearTexImage(texture.getId(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, stack.floats(1.0F));
                    } else {
                        glClearBufferfv(GL_DEPTH, 0, stack.floats(1.0F));
                    }
                }
            }

            if (oldFbo != this.id) {
                GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, oldFbo);
            }
        }

        if (Minecraft.ON_OSX) {
            glGetError();
        }
    }

    private void setDrawBuffers(int[] buffers) {
        int oldFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
        if (oldFbo != this.id) {
            this.bind(false);
        }
        glDrawBuffers(buffers);
        if (oldFbo != this.id) {
            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, oldFbo);
        }
    }

    @Override
    public void resetDrawBuffers() {
        if (Arrays.mismatch(this.currentDrawBuffers, this.drawBuffers) >= 0) {
            this.currentDrawBuffers = this.drawBuffers;
            this.setDrawBuffers(this.drawBuffers);
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
            this.setDrawBuffers(buffers);
        }
    }

    @Override
    public void resolveToFbo(int id, int width, int height, int mask, int filtering) {
        int oldRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int oldDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

        this.bindRead();
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, id);
        glBlitFramebuffer(0, 0, this.getWidth(), this.getHeight(), 0, 0, width, height, mask, filtering);

        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, oldRead);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDraw);
    }

    @Override
    public void resolveToAdvancedFbo(AdvancedFbo target, int mask, int filtering) {
        int oldRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int oldDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

        this.bindRead();
        target.bindDraw(false);
        glBlitFramebuffer(0, 0, this.getWidth(), this.getHeight(), 0, 0, target.getWidth(), target.getHeight(), mask, filtering);

        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, oldRead);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDraw);
    }

    @Override
    public void resolveToRenderTarget(RenderTarget target, int mask, int filtering) {
        int oldRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int oldDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

        this.bindRead();
        RenderTargetExtension extension = (RenderTargetExtension) target;
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, extension.veil$getFramebuffer());
        glBlitFramebuffer(0, 0, this.getWidth(), this.getHeight(), 0, 0, extension.veil$getWidth(), extension.veil$getHeight(), mask, filtering);

        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, oldRead);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDraw);
    }

    @Override
    public void setColorAttachmentTexture(int attachment, int textureId, int layer) {
        int old = glGetInteger(GL_FRAMEBUFFER_BINDING);
        if (old != this.id) {
            this.bind(false);
        }
        super.setColorAttachmentTexture(attachment, textureId, layer);
        if (old != this.id) {
            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, old);
        }
    }

    @Override
    public void setDepthAttachmentTexture(int textureId, int layer) {
        int old = glGetInteger(GL_FRAMEBUFFER_BINDING);
        if (old != this.id) {
            this.bind(false);
        }
        super.setDepthAttachmentTexture(textureId, layer);
        if (old != this.id) {
            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, old);
        }
    }
}
