package foundry.veil.mixin.debug.client;

import com.mojang.blaze3d.platform.GlDebug;
import foundry.veil.VeilDebugHooks;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlDebug.class)
public class DebugGlDebugMixin {

    @Inject(method = "printDebugLog", at = @At("RETURN"))
    private static void onGLError(int source, int type, int id, int severity, int length, long messagePointer, long userParam, CallbackInfo ci) {
        VeilDebugHooks.onGLError(source, type, id, severity, GLDebugMessageCallback.getMessage(length, messagePointer), userParam);
    }
}
