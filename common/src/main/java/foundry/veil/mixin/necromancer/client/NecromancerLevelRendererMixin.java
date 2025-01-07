package foundry.veil.mixin.necromancer.client;

import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.necromancer.animation.Animator;
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
public class NecromancerLevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @SuppressWarnings("rawtypes")
    @Inject(method = "tick", at = @At("HEAD"))
    private void veil$levelRenderTick(CallbackInfo ci) {
        if (this.level == null) {
            return;
        }

        for (Entity entity : this.level.entitiesForRendering()) {
            if (entity instanceof SkeletonParent parent) {
                Animator animator = parent.getAnimator();
                if (animator != null) {
                    animator.tick();
                }
            }
        }
    }
}
