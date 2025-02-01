package foundry.veil.impl.client.render.wrapper;

import com.mojang.blaze3d.pipeline.RenderTarget;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.ext.RenderTargetExtension;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryStack;

import java.util.function.Supplier;

import static org.lwjgl.opengl.ARBClearTexture.glClearTexImage;
import static org.lwjgl.opengl.ARBDirectStateAccess.glBlitNamedFramebuffer;
import static org.lwjgl.opengl.ARBDirectStateAccess.glClearNamedFramebufferfv;
import static org.lwjgl.opengl.GL30C.*;

/**
 * Direct-state implementation of {@link VanillaAdvancedFboWrapper}.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class DSAVanillaAdvancedFboWrapper extends VanillaAdvancedFboWrapper {

    public DSAVanillaAdvancedFboWrapper(Supplier<RenderTarget> renderTargetSupplier) {
        super(renderTargetSupplier);
    }

    @Override
    public void clear(float red, float green, float blue, float alpha, int clearMask, int... clearBuffers) {
        if (clearMask == 0) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            boolean clearTex = VeilRenderSystem.clearTextureSupported();

            RenderTarget renderTarget = this.toRenderTarget();
            if ((clearMask & GL_COLOR_BUFFER_BIT) != 0) {
                if (clearTex) {
                    glClearTexImage(renderTarget.getColorTextureId(), 0, GL_RGBA, GL_FLOAT, stack.floats(red, green, blue, alpha));
                } else {
                    glClearNamedFramebufferfv(renderTarget.getColorTextureId(), GL_COLOR, 0, stack.floats(red, green, blue, alpha));
                }
            }

            if ((clearMask & GL_DEPTH_BUFFER_BIT) != 0 && renderTarget.useDepth) {
                if (clearTex) {
                    glClearTexImage(renderTarget.getDepthTextureId(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, stack.floats(1.0F));
                } else {
                    glClearNamedFramebufferfv(renderTarget.getDepthTextureId(), GL_DEPTH, 0, stack.floats(1.0F));
                }
            }
        }

        if (Minecraft.ON_OSX) {
            glGetError();
        }
    }

    @Override
    public void resolveToFbo(int id, int width, int height, int mask, int filtering) {
        glBlitNamedFramebuffer(this.getId(), id, 0, 0, this.getWidth(), this.getHeight(), 0, 0, width, height, mask, filtering);
    }

    @Override
    public void resolveToAdvancedFbo(AdvancedFbo target, int mask, int filtering) {
        glBlitNamedFramebuffer(this.getId(), target.getId(), 0, 0, this.getWidth(), this.getHeight(), 0, 0, target.getWidth(), target.getHeight(), mask, filtering);
    }

    @Override
    public void resolveToRenderTarget(RenderTarget target, int mask, int filtering) {
        RenderTargetExtension extension = (RenderTargetExtension) target;
        glBlitNamedFramebuffer(this.getId(), extension.veil$getFramebuffer(), 0, 0, this.getWidth(), this.getHeight(), 0, 0, extension.veil$getWidth(), extension.veil$getHeight(), mask, filtering);
    }
}
