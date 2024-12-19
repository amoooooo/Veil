package foundry.veil.mixin.client.pipeline;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.impl.client.render.pipeline.VeilFirstPersonRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    Minecraft minecraft;

    @Unique
    private final Vector3f veil$cameraBobOffset = new Vector3f();

    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void renderLevelStart(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!this.minecraft.options.bobView().get()) {
            VeilRenderSystem.setCameraBobOffset(this.veil$cameraBobOffset.set(0));
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"))
    public void bobViewSetup(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        this.veil$cameraBobOffset.set(0);
    }

    @Inject(method = "bobView", at = @At("TAIL"))
    public void bobViewClear(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        VeilRenderSystem.setCameraBobOffset(this.veil$cameraBobOffset);
    }

    @WrapOperation(method = "bobView", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    public void translateBob(PoseStack instance, float x, float y, float z, Operation<Void> original) {
        this.veil$cameraBobOffset.sub(x, y, z);
        original.call(instance, x, y, z);
    }

    @Inject(method = "resize", at = @At(value = "HEAD"))
    public void veil$resizeListener(int pWidth, int pHeight, CallbackInfo ci) {
        VeilRenderSystem.resize(pWidth, pHeight);
        VeilFirstPersonRenderer.free(); // The old texture is deleted, so we have to remake the framebuffer
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    public void veil$renderPost(DeltaTracker pDeltaTracker, boolean pRenderLevel, CallbackInfo ci) {
        VeilRenderSystem.renderPost();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Lighting;setupFor3DItems()V", shift = At.Shift.AFTER))
    public void veil$updateGuiCamera(DeltaTracker pDeltaTracker, boolean pRenderLevel, CallbackInfo ci) {
        if (Veil.platform().hasErrors()) {
            return;
        }

        VeilRenderer renderer = VeilRenderSystem.renderer();
        renderer.getCameraMatrices().updateGui();
        renderer.getGuiInfo().update();
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void veil$unbindGuiCamera(DeltaTracker pDeltaTracker, boolean pRenderLevel, CallbackInfo ci) {
        if (Veil.platform().hasErrors()) {
            return;
        }

        VeilRenderSystem.renderer().getGuiInfo().unbind();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V", shift = At.Shift.BEFORE))
    public void veil$bindFirstPerson(DeltaTracker pDeltaTracker, CallbackInfo ci) {
        VeilFirstPersonRenderer.bind();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    public void veil$unbindFirstPerson(DeltaTracker pDeltaTracker, CallbackInfo ci) {
        VeilFirstPersonRenderer.unbind();
    }

    @Inject(method = "close", at = @At("TAIL"))
    public void veil$free(CallbackInfo ci) {
        VeilFirstPersonRenderer.free();
    }
}