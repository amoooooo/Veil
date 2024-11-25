package foundry.veil.mixin.client.dynamicbuffer;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.BitSet;

@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer$AmbientOcclusionFace")
public class AmbientOcclusionFaceMixin {

    @Shadow
    @Final
    float[] brightness;

    @Inject(method = "calculate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockAndTintGetter;getShade(Lnet/minecraft/core/Direction;Z)F", shift = At.Shift.BEFORE))
    public void removeShade(BlockAndTintGetter blockAndTintGetter, BlockState blockState, BlockPos blockPos, Direction direction, float[] fs, BitSet bitSet, boolean bl, CallbackInfo ci) {
        if (!VeilRenderSystem.renderer().getLightRenderer().isAmbientOcclusionEnabled()) {
            Arrays.fill(this.brightness, 1.0F);
        }
    }
}
