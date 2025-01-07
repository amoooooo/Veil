package foundry.veil.mixin.rendertype.client;

import foundry.veil.api.client.registry.RenderTypeShardRegistry;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderType.class)
public class RenderTypeMixin {

    @Inject(method = "create(Ljava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;IZZLnet/minecraft/client/renderer/RenderType$CompositeState;)Lnet/minecraft/client/renderer/RenderType$CompositeRenderType;", at = @At("RETURN"))
    private static void injectDeferred(CallbackInfoReturnable<RenderType.CompositeRenderType> cir) {
        RenderTypeShardRegistry.inject(cir.getReturnValue());
    }
}
