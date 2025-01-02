package foundry.veil.mixin.client.pipeline;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Window.class)
public class WindowMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", ordinal = 2, remap = false), index = 1)
    public int captureMajorVersion(int hint, @Share("majorGLVersion") LocalIntRef majorGLVersion) {
        majorGLVersion.set(hint);
        return hint;
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", ordinal = 3, remap = false), index = 1)
    public int captureMinorVersion(int hint, @Share("majorGLVersion") LocalIntRef majorGLVersion) {
        return majorGLVersion.get() == 3 ? Math.max(3, hint) : hint;
    }
}
