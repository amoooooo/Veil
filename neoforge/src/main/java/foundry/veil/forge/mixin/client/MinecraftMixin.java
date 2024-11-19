package foundry.veil.forge.mixin.client;

import foundry.veil.forge.event.ForgeFreeNativeResourcesEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;shutdownExecutors()V", shift = At.Shift.BEFORE))
    public void close(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new ForgeFreeNativeResourcesEvent());
    }
}
