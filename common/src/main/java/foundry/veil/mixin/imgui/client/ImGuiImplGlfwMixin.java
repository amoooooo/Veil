package foundry.veil.mixin.imgui.client;

import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(value = ImGuiImplGlfw.class, remap = false)
public class ImGuiImplGlfwMixin {

    @Inject(method = "updateMousePosAndButtons", at = @At(value = "INVOKE", target = "Limgui/ImGui;getPlatformIO()Limgui/ImGuiPlatformIO;"), cancellable = true)
    public void updateMousePosAndButtons(CallbackInfo ci) {
        if (Minecraft.getInstance().mouseHandler.isMouseGrabbed()) {
            ci.cancel();
        }
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateStandardCursor(I)J", ordinal = 5))
    public int getResizeNESWCursor(int shape) {
        return GLFW_RESIZE_NESW_CURSOR;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateStandardCursor(I)J", ordinal = 6))
    public int getResizeNWSECursor(int shape) {
        return GLFW_RESIZE_NWSE_CURSOR;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateStandardCursor(I)J", ordinal = 8))
    public int getNotAllowedCursor(int shape) {
        return GLFW_NOT_ALLOWED_CURSOR;
    }
}
