package foundry.veil.mixin.performance.client;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.ext.PerformanceRenderTargetExtension;
import foundry.veil.ext.RenderTargetExtension;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.NVDrawTexture;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.ARBClearTexture.glClearTexImage;
import static org.lwjgl.opengl.ARBCopyImage.glCopyImageSubData;
import static org.lwjgl.opengl.ARBDirectStateAccess.glBlitNamedFramebuffer;
import static org.lwjgl.opengl.ARBDirectStateAccess.glClearNamedFramebufferfv;
import static org.lwjgl.opengl.GL11C.*;

@Mixin(RenderTarget.class)
public abstract class PerformanceRenderTargetMixin implements PerformanceRenderTargetExtension {

    @Unique
    private static final ResourceLocation veil$BLIT_SHADER = Veil.veilPath("blit_screen");

    @Shadow
    public int frameBufferId;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    @Final
    private float[] clearChannels;

    @Shadow
    @Final
    public boolean useDepth;

    @Shadow
    public abstract int getColorTextureId();

    @Shadow
    public abstract int getDepthTextureId();

    @Shadow
    public abstract void bindWrite(boolean setViewport);

    @Shadow
    public abstract void unbindWrite();

    @SuppressWarnings("ConstantValue")
    @Inject(method = "copyDepthFrom", at = @At("HEAD"), cancellable = true)
    public void copyDepthFrom(RenderTarget otherTarget, CallbackInfo ci) {
        if (!(((Object) this.getClass()) instanceof MainTarget) || (((Object) this.getClass()) instanceof TextureTarget)) {
            return;
        }

        if (!this.useDepth || !otherTarget.useDepth) {
            ci.cancel();
            return;
        }

        if (VeilRenderSystem.copyImageSupported() && this.width == otherTarget.width && this.height == otherTarget.height) {
            ci.cancel();
            glCopyImageSubData(otherTarget.getDepthTextureId(), GL_TEXTURE_2D, 0, 0, 0, 0, this.getDepthTextureId(), GL_TEXTURE_2D, 0, 0, 0, 0, this.width, this.height, 1);
        } else if (VeilRenderSystem.directStateAccessSupported()) {
            ci.cancel();
            glBlitNamedFramebuffer(otherTarget.frameBufferId, this.frameBufferId, 0, 0, otherTarget.width, otherTarget.height, 0, 0, this.width, this.height, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        }
    }

    @SuppressWarnings("ConstantValue")
    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    public void clear(boolean clearError, CallbackInfo ci) {
        // Prevent any mods that extend render target from having issues
        if (!(((Object) this.getClass()) instanceof MainTarget) || (((Object) this.getClass()) instanceof TextureTarget)) {
            return;
        }

        boolean clearTex = VeilRenderSystem.clearTextureSupported();
        if (!clearTex && !VeilRenderSystem.directStateAccessSupported()) {
            return;
        }

        ci.cancel();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (clearTex) {
                glClearTexImage(this.getColorTextureId(), 0, GL_RGBA, GL_FLOAT, this.clearChannels);
            } else {
                glClearNamedFramebufferfv(this.frameBufferId, GL_COLOR, 0, this.clearChannels);
            }

            if (this.useDepth) {
                if (clearTex) {
                    glClearTexImage(this.getDepthTextureId(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, stack.floats(1.0F));
                } else {
                    glClearNamedFramebufferfv(this.frameBufferId, GL_DEPTH, 0, stack.floats(1.0F));
                }
            }
        }

        if (clearError) {
            glGetError();
        }
    }

    @Inject(method = "_blitToScreen", at = @At("HEAD"), cancellable = true)
    private void _blitToScreen(int width, int height, boolean disableBlend, CallbackInfo ci) {
        // This is likely to have better power efficiency on NVIDIA graphics cards, so prefer it
        // https://registry.khronos.org/OpenGL/extensions/NV/NV_draw_texture.txt
        if (VeilRenderSystem.nvDrawTextureSupported()) {
            ci.cancel();
            RenderSystem.assertOnRenderThread();
            GlStateManager._colorMask(true, true, true, false);
            GlStateManager._depthMask(false);
            GlStateManager._disableDepthTest();
            if (disableBlend) {
                GlStateManager._disableBlend();
            }

            NVDrawTexture.glDrawTextureNV(this.getColorTextureId(), 0,
                    0, 0, width, height, 0.0F,
                    0.0F, 0.0F, 1.0F, 1.0F);

            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._depthMask(true);
            return;
        }

        if (disableBlend && VeilRenderSystem.directStateAccessSupported()) {
            ci.cancel();
            RenderSystem.assertOnRenderThread();
            GlStateManager._colorMask(true, true, true, false);
            int frameBufferId = ((RenderTargetExtension) this).veil$getFramebuffer();
            glBlitNamedFramebuffer(frameBufferId, 0, 0, 0, this.width, this.height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
            GlStateManager._colorMask(true, true, true, true);
        } else {
            ShaderProgram shader = VeilRenderSystem.setShader(veil$BLIT_SHADER);
            if (shader == null) {
                return;
            }

            ci.cancel();
            RenderSystem.assertOnRenderThread();
            GlStateManager._viewport(0, 0, width, height);
            GlStateManager._colorMask(true, true, true, false);
            GlStateManager._depthMask(false);
            GlStateManager._disableDepthTest();
            if (disableBlend) {
                GlStateManager._disableBlend();
            }

            VeilRenderSystem.bindTextures(0, this.getColorTextureId());
            shader.bind();
            VeilRenderSystem.drawScreenQuad();
            ShaderProgram.unbind();

            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._depthMask(true);
        }
    }

    @Override
    public void veil$clearColorBuffer(boolean clearError) {
        RenderSystem.assertOnRenderThreadOrInit();

        int colorTextureId = this.getColorTextureId();
        if (VeilRenderSystem.clearTextureSupported() && glIsTexture(colorTextureId)) {
            glClearTexImage(colorTextureId, 0, GL_RGBA, GL_FLOAT, this.clearChannels);
        } else if (VeilRenderSystem.directStateAccessSupported()) {
            glClearNamedFramebufferfv(this.frameBufferId, GL_COLOR, 0, this.clearChannels);
        } else {
            this.bindWrite(true);
            GlStateManager._clearColor(this.clearChannels[0], this.clearChannels[1], this.clearChannels[2], this.clearChannels[3]);
            GlStateManager._clear(GL_COLOR_BUFFER_BIT, clearError);
            this.unbindWrite();
        }

        if (clearError) {
            glGetError();
        }
    }
}
