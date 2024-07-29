package foundry.veil.fabric.mixin.client;

import com.google.common.collect.ImmutableList;
import foundry.veil.Veil;
import foundry.veil.VeilClient;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.fabric.FabricRenderTypeStageHandler;
import foundry.veil.fabric.event.*;
import foundry.veil.mixin.accessor.RenderStateShardAccessor;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;resizeDisplay()V", shift = At.Shift.BEFORE))
    public void init(GameConfig gameConfig, CallbackInfo ci) {
        VeilClient.initRenderer();
        FabricVeilRendererEvent.EVENT.invoker().onVeilRendererAvailable(VeilRenderSystem.renderer());
        FabricVeilRegisterFixedBuffersEvent.EVENT.invoker().onRegisterFixedBuffers(FabricRenderTypeStageHandler::register);
    }


    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setupDefaultState(IIII)V"))
    public void registerBlockLayers(GameConfig gameConfig, CallbackInfo ci) {
        ImmutableList.Builder<RenderType> blockLayers = ImmutableList.builder();
        FabricVeilRegisterBlockLayerEvent.EVENT.invoker().onRegisterBlockLayers(renderType -> {
            if (Veil.platform().isDevelopmentEnvironment() && renderType.bufferSize() > RenderType.SMALL_BUFFER_SIZE) {
                Veil.LOGGER.warn("Block render layer '{}' uses a large buffer size: {}. If this is intended you can ignore this message", ((RenderStateShardAccessor) renderType).getName(), renderType.bufferSize());
            }
            blockLayers.add(renderType);
        });
        FabricRenderTypeStageHandler.setBlockLayers(blockLayers);
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;shutdownExecutors()V", shift = At.Shift.BEFORE))
    public void close(CallbackInfo ci) {
        FabricFreeNativeResourcesEvent.EVENT.invoker().onFree();
    }
}
