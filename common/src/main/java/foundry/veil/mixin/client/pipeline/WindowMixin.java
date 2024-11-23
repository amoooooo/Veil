package foundry.veil.mixin.client.pipeline;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Window.class)
public class WindowMixin {

    @Unique
    private int veil$majorGLVersion;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", ordinal = 2), index = 1)
    public int captureMajorVersion(int hint) {
        this.veil$majorGLVersion = hint;
        return hint;
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", ordinal = 3), index = 1)
    public int captureMinorVersion(int hint) {
        return this.veil$majorGLVersion == 3 ? Math.max(3, hint) : hint;
    }
}
