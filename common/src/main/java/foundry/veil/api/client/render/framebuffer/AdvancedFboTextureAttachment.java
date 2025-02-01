package foundry.veil.api.client.render.framebuffer;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.ext.VeilDebug;
import foundry.veil.api.client.render.texture.TextureFilter;
import foundry.veil.impl.client.render.framebuffer.AdvancedFboImpl;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.ARBDirectStateAccess.glNamedFramebufferTexture;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL30C.*;

/**
 * An attachment for an {@link AdvancedFboImpl} that represents a color texture buffer.
 *
 * @author Ocelot
 */
public class AdvancedFboTextureAttachment extends AbstractTexture implements AdvancedFboAttachment {

    private final int attachmentType;
    private final int format;
    private final int texelFormat;
    private final int dataType;
    private final int width;
    private final int height;
    private final int mipmapLevels;
    private final TextureFilter filter;
    private final String name;

    /**
     * Creates a new attachment that adds a texture.
     *
     * @param attachmentType The attachment point to put this on
     * @param format         The format of the image data
     * @param texelFormat    The format of the image texel data
     * @param dataType       The type of data to store in the texture
     * @param width          The width of the attachment
     * @param height         The height of the attachment
     * @param mipmapLevels   The number of mipmaps levels to have
     * @param name           The custom name of this attachment for shader references
     */
    public AdvancedFboTextureAttachment(int attachmentType,
                                        int format,
                                        int texelFormat,
                                        int dataType,
                                        int width,
                                        int height,
                                        int mipmapLevels,
                                        TextureFilter filter,
                                        @Nullable String name) {
        this.attachmentType = attachmentType;
        this.format = format;
        this.texelFormat = texelFormat;
        this.dataType = dataType;
        this.width = width;
        this.height = height;
        this.mipmapLevels = mipmapLevels;
        this.filter = filter;
        this.name = name;
    }

    @Override
    public void create() {
        this.bindAttachment();
        this.setFilter(this.filter.blur(), this.filter.mipmap());

        if (this.name != null) {
            VeilDebug.get().objectLabel(GL_TEXTURE, this.getId(), "Texture " + this.name);
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, this.mipmapLevels - 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, this.mipmapLevels - 1);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0F);
        this.filter.applyToTextureTarget(GL_TEXTURE_2D);

        for (int i = 0; i < this.mipmapLevels; i++) {
            glTexImage2D(GL_TEXTURE_2D, i, this.format, this.width >> i, this.height >> i, 0, this.texelFormat, this.dataType, (ByteBuffer) null);
        }
    }

    @Override
    public void attach(int framebuffer, int attachment) {
        Validate.isTrue(this.attachmentType < GL_DEPTH_ATTACHMENT || attachment == 0, "Only one depth buffer attachment is supported.");

        if (VeilRenderSystem.directStateAccessSupported()) {
            glNamedFramebufferTexture(framebuffer,
                    this.attachmentType + attachment,
                    this.getId(),
                    0); // Only draw into the first level
        } else {
            glFramebufferTexture2D(GL_FRAMEBUFFER,
                    this.attachmentType + attachment,
                    GL_TEXTURE_2D,
                    this.getId(),
                    0); // Only draw into the first level
        }
    }

    @Override
    public AdvancedFboTextureAttachment clone() {
        return new AdvancedFboTextureAttachment(this.attachmentType, this.format, this.texelFormat, this.dataType, this.width, this.height, this.mipmapLevels, this.filter, this.name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        AdvancedFboTextureAttachment that = (AdvancedFboTextureAttachment) o;
        return this.attachmentType == that.attachmentType && this.format == that.format && this.texelFormat == that.texelFormat && this.dataType == that.dataType && this.width == that.width && this.height == that.height && this.mipmapLevels == that.mipmapLevels && this.filter.equals(that.filter) && Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        int result = this.attachmentType;
        result = 31 * result + this.format;
        result = 31 * result + this.texelFormat;
        result = 31 * result + this.dataType;
        result = 31 * result + this.width;
        result = 31 * result + this.height;
        result = 31 * result + this.mipmapLevels;
        result = 31 * result + this.filter.hashCode();
        result = 31 * result + Objects.hashCode(this.name);
        return result;
    }

    @Override
    public void bindAttachment() {
        this.bind();
    }

    @Override
    public void unbindAttachment() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> RenderSystem.bindTexture(0));
        } else {
            RenderSystem.bindTexture(0);
        }
    }

    @Override
    public int getAttachmentType() {
        return this.attachmentType;
    }

    @Override
    public int getFormat() {
        return this.format;
    }

    public int getTexelFormat() {
        return this.texelFormat;
    }

    public int getDataType() {
        return this.dataType;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    @Override
    public int getLevels() {
        return this.mipmapLevels;
    }

    public TextureFilter getFilter() {
        return this.filter;
    }

    @Override
    public boolean canSample() {
        return true;
    }

    @Override
    public @Nullable String getName() {
        return this.name;
    }

    @Override
    public void free() {
        this.releaseId();
    }

    @Override
    public void load(ResourceManager manager) {
    }
}
