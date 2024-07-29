package foundry.veil.mixin.client.stage;

import foundry.veil.api.client.render.rendertype.VeilRenderTypeAssigner;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {
    @Inject(
        method = {"getRenderType(Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/client/renderer/RenderType;"},
        at = @At("HEAD"),
        cancellable = true)
    private static void fixRenderTypeFromBlockState(BlockState blockState, boolean bl, CallbackInfoReturnable<RenderType> cir) {
        VeilRenderTypeAssigner
            .forBlock(blockState)
            .ifPresent(cir::setReturnValue);
    }

    @Inject(
        method = {"getRenderType(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/client/renderer/RenderType;"},
        at = @At("HEAD"),
        cancellable = true)
    private static void useCustomItemStackRenderType(ItemStack itemStack, boolean bl, CallbackInfoReturnable<RenderType> cir) {
        VeilRenderTypeAssigner
            .forItem(itemStack)
            .ifPresent(cir::setReturnValue);
    }
}
