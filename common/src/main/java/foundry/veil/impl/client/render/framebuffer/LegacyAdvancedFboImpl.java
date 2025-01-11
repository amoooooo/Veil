package foundry.veil.impl.client.render.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboAttachment;
import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import foundry.veil.ext.RenderTargetExtension;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
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

    public LegacyAdvancedFboImpl(int width, int height, AdvancedFboAttachment[] colorAttachments, @Nullable AdvancedFboAttachment depthAttachment) {
        super(width, height, colorAttachments, depthAttachment);
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

        for (int i = 0; i < this.colorAttachments.length; i++) {
            this.colorAttachments[i].attach(this.id, i);
        }
        if (this.depthAttachment != null) {
            this.depthAttachment.attach(this.id, 0);
        }

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String error = ERRORS.containsKey(status) ? ERRORS.get(status) : "0x" + Integer.toHexString(status).toUpperCase(Locale.ROOT);
            throw new IllegalStateException("Advanced FBO status did not return GL_FRAMEBUFFER_COMPLETE. " + error);
        }

        glDrawBuffers(this.drawBuffers);
        glBindFramebuffer(GL_FRAMEBUFFER, oldFbo);
    }

    @Override
    public void clear(float red, float green, float blue, float alpha, int clearMask, int[] buffers) {
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

            if ((clearMask & GL_DEPTH_BUFFER_BIT) != 0 && this.depthAttachment != null) {
                if (clearTex && this.depthAttachment instanceof AdvancedFboTextureAttachment texture) {
                    glClearTexImage(texture.getId(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, stack.floats(1.0F));
                } else {
                    glClearBufferfv(GL_DEPTH, 0, stack.floats(1.0F));
                }
            }

            if (oldFbo != this.id) {
                glBindFramebuffer(GL_FRAMEBUFFER, oldFbo);
            }
        }

        if (Minecraft.ON_OSX) {
            glGetError();
        }
    }

    @Override
    public void drawBuffers(int... buffers) {
        int oldFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
        if (oldFbo != this.id) {
            this.bind(false);
        }
        glDrawBuffers(this.drawBuffers);
        if (oldFbo != this.id) {
            glBindFramebuffer(GL_FRAMEBUFFER, oldFbo);
        }
    }

    @Override
    public void resolveToFbo(int id, int width, int height, int mask, int filtering) {
        int oldRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int oldDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

        this.bindRead();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, id);
        glBlitFramebuffer(0, 0, this.getWidth(), this.getHeight(), 0, 0, width, height, mask, filtering);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, oldRead);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDraw);
    }

    @Override
    public void resolveToAdvancedFbo(AdvancedFbo target, int mask, int filtering) {
        int oldRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int oldDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

        this.bindRead();
        target.bindDraw(false);
        glBlitFramebuffer(0, 0, this.getWidth(), this.getHeight(), 0, 0, target.getWidth(), target.getHeight(), mask, filtering);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, oldRead);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDraw);
    }

    @Override
    public void resolveToRenderTarget(RenderTarget target, int mask, int filtering) {
        int oldRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int oldDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

        this.bindRead();
        RenderTargetExtension extension = (RenderTargetExtension) target;
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, extension.veil$getFramebuffer());
        glBlitFramebuffer(0, 0, this.getWidth(), this.getHeight(), 0, 0, extension.veil$getWidth(), extension.veil$getHeight(), mask, filtering);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, oldRead);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDraw);
    }

    @Override
    public void setColorAttachmentTexture(int attachment, int textureId, int layer) {
        int oldFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
        if (oldFbo != this.id) {
            this.bind(false);
        }
        super.setColorAttachmentTexture(attachment, textureId, layer);
        if (oldFbo != this.id) {
            glBindFramebuffer(GL_FRAMEBUFFER, oldFbo);
        }
    }

    @Override
    public void setDepthAttachmentTexture(int textureId, int layer) {
        int oldFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
        this.bind(false);
        super.setDepthAttachmentTexture(textureId, layer);
        glBindFramebuffer(GL_FRAMEBUFFER, oldFbo);
    }
}
