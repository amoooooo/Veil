package foundry.veil.mixin.debug.client;

import foundry.veil.Veil;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Minecraft.class)
public class DebugMinecraftMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V", remap = false), index = 0)
    private int modifyDebugVerbosity(int debugVerbosity) {
        return Veil.DEBUG ? 100 : debugVerbosity;
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V", remap = false), index = 1)
    private boolean modifySynchronousDebug(boolean synchronous) {
        return synchronous || Veil.DEBUG;
    }
}
