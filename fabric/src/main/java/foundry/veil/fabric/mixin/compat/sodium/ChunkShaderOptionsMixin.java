package foundry.veil.fabric.mixin.compat.sodium;

import foundry.veil.ext.sodium.ChunkShaderOptionsExtension;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkShaderOptions.class)
public class ChunkShaderOptionsMixin implements ChunkShaderOptionsExtension {

    @Unique
    private int veil$activeBuffers;

    @Override
    public int veil$getActiveBuffers() {
        return this.veil$activeBuffers;
    }

    @Override
    public void veil$setActiveBuffers(int activeBuffers) {
        this.veil$activeBuffers = activeBuffers;
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true, remap = false)
    public void equals(Object o, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && o instanceof ChunkShaderOptionsExtension options && options.veil$getActiveBuffers() != this.veil$activeBuffers) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hashCode", at = @At("RETURN"), cancellable = true, remap = false)
    public void hashCode(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(31 * cir.getReturnValueI() + this.veil$activeBuffers);
    }
}
