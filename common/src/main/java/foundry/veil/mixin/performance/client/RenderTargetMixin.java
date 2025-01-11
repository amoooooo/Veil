package foundry.veil.mixin.performance.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import foundry.veil.api.client.render.VeilRenderSystem;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.ARBClearTexture.glClearTexImage;
import static org.lwjgl.opengl.ARBDirectStateAccess.glBlitNamedFramebuffer;
import static org.lwjgl.opengl.ARBDirectStateAccess.glClearNamedFramebufferfv;
import static org.lwjgl.opengl.GL11C.*;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {

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

    @Inject(method = "copyDepthFrom", at = @At("HEAD"), cancellable = true)
    public void copyDepthFrom(RenderTarget otherTarget, CallbackInfo ci) {
        if (!VeilRenderSystem.directStateAccessSupported()) {
            return;
        }

        ci.cancel();
        glBlitNamedFramebuffer(otherTarget.frameBufferId, this.frameBufferId, 0, 0, otherTarget.width, otherTarget.height, 0, 0, this.width, this.height, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
    }

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    public void clear(boolean clearError, CallbackInfo ci) {
        if (!VeilRenderSystem.directStateAccessSupported()) {
            return;
        }

        ci.cancel();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            boolean clearTex = VeilRenderSystem.clearTextureSupported();

            if (clearTex) {
                glClearTexImage(this.getColorTextureId(), 0, GL_RGBA, GL_FLOAT, this.clearChannels);
            } else {
                glClearNamedFramebufferfv(this.getColorTextureId(), GL_COLOR, 0, this.clearChannels);
            }

            if (this.useDepth) {
                if (clearTex) {
                    glClearTexImage(this.getDepthTextureId(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, stack.floats(1.0F));
                } else {
                    glClearNamedFramebufferfv(this.getDepthTextureId(), GL_DEPTH, 0, stack.floats(1.0F));
                }
            }
        }

        if (clearError) {
            glGetError();
        }
    }
}
