package foundry.veil.mixin.client.pipeline;

import com.mojang.blaze3d.pipeline.RenderTarget;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.ext.RenderTargetExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.opengl.GL30C.*;

@Mixin(RenderTarget.class)
public class RenderTargetMixin implements RenderTargetExtension {

    @Shadow
    public int frameBufferId;

    @Unique
    private AdvancedFbo veil$wrapper;

    @Override
    public void veil$setWrapper(@Nullable AdvancedFbo fbo) {
        this.veil$wrapper = fbo;
    }

    @Override
    public void veil$bindDrawFramebuffer() {
        if (this.veil$wrapper != null) {
            this.veil$wrapper.bindDraw(false);
        } else {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.frameBufferId);
        }
    }

    @Override
    public void veil$bindReadFramebuffer() {
        if (this.veil$wrapper != null) {
            this.veil$wrapper.bindRead();
        } else {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, this.frameBufferId);
        }
    }

    @Override
    public int veil$getTexture(int buffer) {
        if (this.veil$wrapper != null && this.veil$wrapper.isColorTextureAttachment(buffer)) {
            this.veil$wrapper.getColorTextureAttachment(buffer).getId();
        }
        return -1;
    }

    @Inject(method = "bindRead", at = @At("HEAD"), cancellable = true)
    public void bindRead(CallbackInfo ci) {
        if (this.veil$wrapper != null) {
            if (this.veil$wrapper.isColorTextureAttachment(0)) {
                this.veil$wrapper.getColorTextureAttachment(0).bind();
            }
            ci.cancel();
        }
    }

    @Inject(method = "bindWrite", at = @At("HEAD"), cancellable = true)
    public void bindWrite(CallbackInfo ci) {
        if (this.veil$wrapper != null) {
            this.veil$wrapper.bind(true);
            ci.cancel();
        }
    }

    @Inject(method = "getColorTextureId", at = @At("HEAD"), cancellable = true)
    public void getColorTextureId(CallbackInfoReturnable<Integer> cir) {
        if (this.veil$wrapper != null) {
            cir.setReturnValue(this.veil$wrapper.getColorTextureAttachment(0).getId());
        }
    }

    @Inject(method = "getDepthTextureId", at = @At("HEAD"), cancellable = true)
    public void getDepthTextureId(CallbackInfoReturnable<Integer> cir) {
        if (this.veil$wrapper != null) {
            cir.setReturnValue(this.veil$wrapper.getDepthTextureAttachment().getId());
        }
    }
}
