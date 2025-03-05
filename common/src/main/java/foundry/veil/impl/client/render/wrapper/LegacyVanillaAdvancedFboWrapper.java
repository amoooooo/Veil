package foundry.veil.impl.client.render.wrapper;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.ext.RenderTargetExtension;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

import static org.lwjgl.opengl.GL30C.*;

/**
 * Legacy implementation of {@link VanillaAdvancedFboWrapper}.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class LegacyVanillaAdvancedFboWrapper extends VanillaAdvancedFboWrapper {

    public LegacyVanillaAdvancedFboWrapper(Supplier<RenderTarget> renderTargetSupplier) {
        super(renderTargetSupplier);
    }

    @Override
    public void clear(float red, float green, float blue, float alpha, int buffers, int... clearBuffers) {
        if (buffers == 0) {
            return;
        }

        int old = glGetInteger(GL_FRAMEBUFFER_BINDING);
        int id = this.getId();
        if (old != id) {
            this.bind(false);
        }

        if ((buffers & GL_COLOR_BUFFER_BIT) != 0) {
            RenderSystem.clearColor(red, green, blue, alpha);
        }
        if ((buffers & GL_DEPTH_BUFFER_BIT) != 0) {
            RenderSystem.clearDepth(1.0);
        }
        RenderSystem.clear(buffers, Minecraft.ON_OSX);

        if (old != id) {
            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, old);
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
}
