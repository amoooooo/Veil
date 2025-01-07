package foundry.veil.mixin.imgui.client;

import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ImGuiImplGlfw.class, remap = false)
public class ImGuiImplGlfwMixin {

    @Inject(method = "updateMousePosAndButtons", at = @At(value = "INVOKE", target = "Limgui/ImGui;getPlatformIO()Limgui/ImGuiPlatformIO;"), cancellable = true)
    public void updateMousePosAndButtons(CallbackInfo ci) {
        if (Minecraft.getInstance().mouseHandler.isMouseGrabbed()) {
            ci.cancel();
        }
    }
}
