package foundry.veil.api.client.render.framebuffer;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.ext.VeilDebug;
import foundry.veil.impl.client.render.framebuffer.AdvancedFboImpl;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * An attachment for an {@link AdvancedFboImpl} that represents a depth render buffer.
 *
 * @author Ocelot
 */
public class AdvancedFboRenderAttachment implements AdvancedFboAttachment {

    public static final int MAX_SAMPLES = glGetInteger(GL_MAX_SAMPLES);

    private int id;
    private final int attachmentType;
    private final int attachmentFormat;
    private final int width;
    private final int height;
    private final int samples;

    /**
     * Creates a new attachment that adds a renderbuffer.
     *
     * @param attachmentType   The attachment point to put this on
     * @param attachmentFormat The format of the attachment data
     * @param width            The width of the attachment
     * @param height           The height of the attachment
     * @param samples          The number of samples to have. It must be between<code>1</code>
     *                         and {@link AdvancedFboRenderAttachment#MAX_SAMPLES}
     */
    public AdvancedFboRenderAttachment(int attachmentType, int attachmentFormat, int width, int height, int samples) {
        this.attachmentType = attachmentType;
        this.attachmentFormat = attachmentFormat;
        this.width = width;
        this.height = height;
        Validate.inclusiveBetween(1, AdvancedFboRenderAttachment.MAX_SAMPLES, samples);
        this.samples = samples;
    }

    @Override
    public void create() {
        if (VeilRenderSystem.directStateAccessSupported()) {
            if (this.samples == 1) {
                glNamedRenderbufferStorage(
                        this.id,
                        this.attachmentFormat,
                        this.width,
                        this.height);
            } else {
                glNamedRenderbufferStorageMultisample(
                        this.id,
                        this.samples,
                        this.attachmentFormat,
                        this.width,
                        this.height);
            }
        } else {
            this.bindAttachment();
            if (this.samples == 1) {
                glRenderbufferStorage(
                        GL_RENDERBUFFER,
                        this.attachmentFormat,
                        this.width,
                        this.height);
            } else {
                glRenderbufferStorageMultisample(
                        GL_RENDERBUFFER,
                        this.samples,
                        this.attachmentFormat,
                        this.width,
                        this.height);
            }
            this.unbindAttachment();
        }
    }

    @Override
    public void attach(AdvancedFbo framebuffer, int attachment) {
        Validate.isTrue(this.attachmentType != GL_DEPTH_ATTACHMENT || attachment == 0, "Only one depth buffer attachment is supported.");

        int id = this.getId();
        if (VeilRenderSystem.directStateAccessSupported()) {
            glNamedFramebufferRenderbuffer(framebuffer.getId(), this.attachmentType, GL_RENDERBUFFER, id);
        } else {
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, this.attachmentType, GL_RENDERBUFFER, id);
        }

        String debugLabel = framebuffer.getDebugLabel();
        if (debugLabel != null) {
            if (this.attachmentType == GL_DEPTH_ATTACHMENT) {
                VeilDebug.get().objectLabel(GL_RENDERBUFFER, id, "Advanced Fbo " + debugLabel + " Depth Render Buffer");
            } else {
                VeilDebug.get().objectLabel(GL_RENDERBUFFER, id, "Advanced Fbo " + debugLabel + " Render Buffer " + attachment);
            }
        }
    }

    @Override
    public void bindAttachment() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> glBindRenderbuffer(GL_RENDERBUFFER, this.getId()));
        } else {
            glBindRenderbuffer(GL_RENDERBUFFER, this.getId());
        }
    }

    @Override
    public void unbindAttachment() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> glBindRenderbuffer(GL_RENDERBUFFER, 0));
        } else {
            glBindRenderbuffer(GL_RENDERBUFFER, 0);
        }
    }

    /**
     * @return The OpenGL renderbuffer id of this attachment
     */
    public int getId() {
        RenderSystem.assertOnRenderThreadOrInit();
        if (this.id == 0) {
            this.id = VeilRenderSystem.directStateAccessSupported() ? glCreateRenderbuffers() : glGenRenderbuffers();
        }

        return this.id;
    }

    @Override
    public int getAttachmentType() {
        return this.attachmentType;
    }

    @Override
    public int getFormat() {
        return this.attachmentFormat;
    }

    @Override
    public int getLevels() {
        return this.samples;
    }

    @Override
    public boolean canSample() {
        return false;
    }

    @Override
    public @Nullable String getName() {
        return null;
    }

    @Override
    public @NotNull AdvancedFboAttachment clone() {
        return new AdvancedFboRenderAttachment(this.attachmentType, this.attachmentFormat, this.width, this.height, this.samples);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        AdvancedFboRenderAttachment that = (AdvancedFboRenderAttachment) o;
        return this.id == that.id && this.attachmentType == that.attachmentType && this.attachmentFormat == that.attachmentFormat && this.width == that.width && this.height == that.height && this.samples == that.samples;
    }

    @Override
    public int hashCode() {
        int result = this.id;
        result = 31 * result + this.attachmentType;
        result = 31 * result + this.attachmentFormat;
        result = 31 * result + this.width;
        result = 31 * result + this.height;
        result = 31 * result + this.samples;
        return result;
    }

    @Override
    public void free() {
        if (this.id != 0) {
            glDeleteRenderbuffers(this.id);
        }
        this.id = 0;
    }
}
