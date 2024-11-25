package foundry.veil.mixin.client.dynamicbuffer;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {

    @ModifyVariable(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;showOnlyReducedInfo()Z", shift = At.Shift.BEFORE), ordinal = 0)
    public List<String> modifyGameInformation(List<String> value) {
        int mask = VeilRenderSystem.renderer().getDynamicBufferManger().getActiveBuffers();
        if (mask != 0) {
            value.add("");
            value.add(ChatFormatting.UNDERLINE + "Veil Active Buffers");
            value.add(Arrays.stream(DynamicBufferType.decode(mask)).map(DynamicBufferType::getName).collect(Collectors.joining(", ")));
        }
        return value;
    }
}
