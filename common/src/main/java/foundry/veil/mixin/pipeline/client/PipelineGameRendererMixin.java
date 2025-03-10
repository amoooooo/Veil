package foundry.veil.mixin.pipeline.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.compat.IrisCompat;
import foundry.veil.impl.client.render.pipeline.VeilBloomRenderer;
import foundry.veil.impl.client.render.pipeline.VeilFirstPersonRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GameRenderer.class)
public class PipelineGameRendererMixin {

    @Shadow
    @Final
    Minecraft minecraft;
    @Shadow
    private boolean panoramicMode;

    @Unique
    private final Vector3f veil$cameraBobOffset = new Vector3f();

    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void renderLevelStart(CallbackInfo ci) {
        if (!this.minecraft.options.bobView().get()) {
            VeilRenderSystem.setCameraBobOffset(this.veil$cameraBobOffset.set(0));
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"))
    public void bobViewSetup(CallbackInfo ci) {
        this.veil$cameraBobOffset.set(0);
    }

    @Inject(method = "bobView", at = @At("TAIL"))
    public void bobViewClear(CallbackInfo ci) {
        VeilRenderSystem.setCameraBobOffset(this.veil$cameraBobOffset);
    }

    @ModifyArgs(method = "bobView", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    public void translateBob(Args args) {
        this.veil$cameraBobOffset.sub(args.get(0), args.get(1), args.get(2));
    }

    @Inject(method = "resize", at = @At(value = "HEAD"))
    public void resizeListener(int pWidth, int pHeight, CallbackInfo ci) {
        VeilRenderSystem.resize(pWidth, pHeight);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V"))
    public boolean wrapRenderPost(LevelRenderer instance) {
        return !VeilLevelPerspectiveRenderer.isRenderingPerspective();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    public void renderPost(CallbackInfo ci) {
        if (!VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            VeilRenderSystem.renderPost(null);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Lighting;setupFor3DItems()V", shift = At.Shift.AFTER))
    public void updateGuiCamera(CallbackInfo ci) {
        if (Veil.platform().hasErrors()) {
            return;
        }

        VeilRenderSystem.renderer().getGuiInfo().update();
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void unbindGuiCamera(CallbackInfo ci) {
        if (Veil.platform().hasErrors()) {
            return;
        }

        VeilRenderSystem.renderer().getGuiInfo().unbind();
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V", remap = false))
    public void bindFirstPerson(int mask, boolean checkError) {
        // Don't try to run first person processing if the hand is hidden
        if (!this.panoramicMode && (IrisCompat.INSTANCE == null || !IrisCompat.INSTANCE.areShadersLoaded())) {
            VeilFirstPersonRenderer.bind(mask);
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    public void unbindFirstPerson(CallbackInfo ci) {
        // Don't try to run first person processing if the hand is hidden
        if (!this.panoramicMode && (IrisCompat.INSTANCE == null || !IrisCompat.INSTANCE.areShadersLoaded())) {
            VeilFirstPersonRenderer.unbind();
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    public void free(CallbackInfo ci) {
        VeilFirstPersonRenderer.free();
        VeilBloomRenderer.free();
    }
}