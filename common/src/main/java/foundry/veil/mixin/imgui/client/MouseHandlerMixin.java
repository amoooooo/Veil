package foundry.veil.mixin.imgui.client;

import foundry.veil.Veil;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    public void mouseButtonCallback(long window, int button, int action, int mods, CallbackInfo ci) {
        try {
            if (Veil.beginImGui().mouseButtonCallback(window, button, action, mods)) {
                ci.cancel();
            }
        } finally {
            Veil.endImGui();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    public void scrollCallback(long window, double xOffset, double yOffset, CallbackInfo ci) {
        try {
            if (Veil.beginImGui().scrollCallback(window, xOffset, yOffset)) {
                ci.cancel();
            }
        } finally {
            Veil.endImGui();
        }
    }

    @Inject(method = "grabMouse", at = @At("HEAD"))
    public void grabMouse(CallbackInfo ci) {
        try {
            Veil.beginImGui().onGrabMouse();
        } finally {
            Veil.endImGui();
        }
    }

    @Inject(method = "xpos", at = @At("HEAD"), cancellable = true)
    public void cancelMouseX(CallbackInfoReturnable<Double> cir) {
        try {
            if (Veil.beginImGui().shouldHideMouse()) {
                cir.setReturnValue(Double.MIN_VALUE);
            }
        } finally {
            Veil.endImGui();
        }
    }

    @Inject(method = "ypos", at = @At("HEAD"), cancellable = true)
    public void cancelMouseY(CallbackInfoReturnable<Double> cir) {
        try {
            if (Veil.beginImGui().shouldHideMouse()) {
                cir.setReturnValue(Double.MIN_VALUE);
            }
        } finally {
            Veil.endImGui();
        }
    }
}
