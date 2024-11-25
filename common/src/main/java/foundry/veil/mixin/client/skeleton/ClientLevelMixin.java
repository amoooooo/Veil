package foundry.veil.mixin.client.skeleton;

import foundry.veil.api.client.necromancer.render.NecromancerEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "addEntity", at = @At("TAIL"))
    private void addEntity(Entity pEntity, CallbackInfo ci) {
        if (this.minecraft.getEntityRenderDispatcher().getRenderer(pEntity) instanceof NecromancerEntityRenderer renderer) {
            renderer.setupEntity(pEntity);
        }
    }
}
