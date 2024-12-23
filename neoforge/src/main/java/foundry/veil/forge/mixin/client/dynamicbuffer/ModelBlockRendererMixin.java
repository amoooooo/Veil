package foundry.veil.forge.mixin.client.dynamicbuffer;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

    @Inject(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", at = @At("HEAD"), remap = false)
    public void captureState(CallbackInfo ci, @Share("switchNormal") LocalBooleanRef switchNormal) {
        switchNormal.set((VeilRenderSystem.renderer().getDynamicBufferManger().getActiveBuffers() & DynamicBufferType.NORMAL.getMask()) != 0);
    }

    @Inject(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", at = @At("RETURN"), remap = false)
    public void clearState(CallbackInfo ci, @Share("switchNormal") LocalBooleanRef switchNormal) {
        switchNormal.set(false);
    }

    @ModifyArg(method = "renderModelFaceFlat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"), index = 6)
    public float modifyShade0(float value, @Share("switchNormal") LocalBooleanRef switchNormal) {
        return switchNormal.get() ? 1.0F : value;
    }

    @ModifyArg(method = "renderModelFaceFlat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"), index = 7)
    public float modifyShade1(float value, @Share("switchNormal") LocalBooleanRef switchNormal) {
        return switchNormal.get() ? 1.0F : value;
    }

    @ModifyArg(method = "renderModelFaceFlat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"), index = 8)
    public float modifyShade2(float value, @Share("switchNormal") LocalBooleanRef switchNormal) {
        return switchNormal.get() ? 1.0F : value;
    }

    @ModifyArg(method = "renderModelFaceFlat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"), index = 9)
    public float modifyShade3(float value, @Share("switchNormal") LocalBooleanRef switchNormal) {
        return switchNormal.get() ? 1.0F : value;
    }

    // TODO allow vertical normals
//    @ModifyVariable(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true, remap = false)
//    public VertexConsumer modifyConsumer(VertexConsumer value, @Share("switchNormal") LocalBooleanRef switchNormal) {
//        return switchNormal.get() ? new VerticalNormalVertexConsumer(value) : value;
//    }
}
