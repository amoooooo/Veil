package foundry.veil.mixin.client.skeleton;

import foundry.veil.api.client.necromancer.SkeletonParent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow @Nullable private ClientLevel level;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void veil$levelRenderTick(CallbackInfo ci) {
        if (this.level == null) return;
        for (Entity entity : this.level.entitiesForRendering()) {
            if (entity instanceof SkeletonParent parent) {
                if (parent.getAnimator() != null) parent.getAnimator().tick(parent);
            }
        }
    }
}
