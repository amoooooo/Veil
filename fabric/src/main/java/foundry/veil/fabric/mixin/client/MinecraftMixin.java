package foundry.veil.fabric.mixin.client;

import com.google.common.collect.ImmutableList;
import foundry.veil.Veil;
import foundry.veil.VeilClient;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.fabric.FabricRenderTypeStageHandler;
import foundry.veil.fabric.event.FabricFreeNativeResourcesEvent;
import foundry.veil.fabric.event.FabricVeilRegisterBlockLayersEvent;
import foundry.veil.fabric.event.FabricVeilRegisterFixedBuffersEvent;
import foundry.veil.fabric.event.FabricVeilRendererAvailableEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;resizeDisplay()V", shift = At.Shift.BEFORE))
    public void init(CallbackInfo ci) {
        VeilClient.initRenderer();
        FabricVeilRendererAvailableEvent.EVENT.invoker().onVeilRendererAvailable(VeilRenderSystem.renderer());
        FabricVeilRegisterFixedBuffersEvent.EVENT.invoker().onRegisterFixedBuffers(FabricRenderTypeStageHandler::register);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setupDefaultState(IIII)V", remap = false))
    public void registerBlockLayers(CallbackInfo ci) {
        ImmutableList.Builder<RenderType> blockLayers = ImmutableList.builder();
        FabricVeilRegisterBlockLayersEvent.EVENT.invoker().onRegisterBlockLayers(renderType -> {
            if (Veil.platform().isDevelopmentEnvironment() && renderType.bufferSize() > RenderType.SMALL_BUFFER_SIZE) {
                Veil.LOGGER.warn("Block render layer '{}' uses a large buffer size: {}. If this is intended you can ignore this message", VeilRenderType.getName(renderType), renderType.bufferSize());
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
