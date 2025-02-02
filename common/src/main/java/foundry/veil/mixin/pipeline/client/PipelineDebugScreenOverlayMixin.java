package foundry.veil.mixin.pipeline.client;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class PipelineDebugScreenOverlayMixin {

    @ModifyVariable(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;showOnlyReducedInfo()Z", shift = At.Shift.BEFORE), ordinal = 0)
    public List<String> modifyGameInformation(List<String> value) {
        VeilRenderSystem.renderer().addDebugInfo(value::add);
        return value;
    }
}
