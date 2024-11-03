package foundry.veil.api.client.render.framebuffer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.impl.client.render.AdvancedFboImpl;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;

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
    private final boolean linear;
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
                                        boolean linear,
                                        @Nullable String name) {
        this.attachmentType = attachmentType;
        this.format = format;
        this.texelFormat = texelFormat;
        this.dataType = dataType;
        this.width = width;
        this.height = height;
        this.mipmapLevels = mipmapLevels;
        this.linear = linear;
        this.name = name;
    }

    @Override
    public void create() {
        this.bindAttachment();
        this.setFilter(this.linear, this.mipmapLevels > 1);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, this.mipmapLevels);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, this.mipmapLevels);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0F);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        for (int i = 0; i <= this.mipmapLevels; i++) {
            GlStateManager._texImage2D(GL_TEXTURE_2D, i, this.format, this.width >> i, this.height >> i, 0, this.texelFormat, this.dataType, null);
        }
        this.unbindAttachment();
    }

    @Override
    public void attach(int attachment) {
        Validate.isTrue(this.attachmentType < GL_DEPTH_ATTACHMENT || attachment == 0, "Only one depth buffer attachment is supported.");
        GlStateManager._glFramebufferTexture2D(GL_FRAMEBUFFER,
                this.attachmentType + attachment,
                GL_TEXTURE_2D,
                this.getId(),
                0); // Only draw into the first level
    }

    @Override
    public AdvancedFboTextureAttachment clone() {
        return new AdvancedFboTextureAttachment(this.attachmentType, this.format, this.texelFormat, this.dataType, this.width, this.height, this.mipmapLevels, this.linear, this.name);
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

    @Override
    public int getLevels() {
        return this.mipmapLevels;
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
