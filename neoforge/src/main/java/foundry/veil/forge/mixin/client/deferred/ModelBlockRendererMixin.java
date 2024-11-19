package foundry.veil.forge.mixin.client.deferred;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.deferred.VeilDeferredRenderer;
import foundry.veil.impl.client.render.deferred.DeferredVertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

    @Unique
    private static final ThreadLocal<Boolean> veil$DEFERRED = ThreadLocal.withInitial(() -> false);

    @Inject(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", at = @At("HEAD"), remap = false)
    public void captureState(BlockAndTintGetter p_234380_, BakedModel p_234381_, BlockState p_234382_, BlockPos p_234383_, PoseStack p_234384_, VertexConsumer p_234385_, boolean p_234386_, RandomSource p_234387_, long p_234388_, int p_234389_, ModelData modelData, RenderType renderType, CallbackInfo ci) {
        VeilDeferredRenderer deferredRenderer = VeilRenderSystem.renderer().getDeferredRenderer();
        veil$DEFERRED.set(deferredRenderer.isEnabled() && deferredRenderer.getRendererState() != VeilDeferredRenderer.RendererState.DISABLED);
    }

    @Inject(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", at = @At("RETURN"), remap = false)
    public void clearState(BlockAndTintGetter p_234380_, BakedModel p_234381_, BlockState p_234382_, BlockPos p_234383_, PoseStack p_234384_, VertexConsumer p_234385_, boolean p_234386_, RandomSource p_234387_, long p_234388_, int p_234389_, ModelData modelData, RenderType renderType, CallbackInfo ci) {
        veil$DEFERRED.set(false);
    }

    @ModifyArg(method = "renderModelFaceFlat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"), index = 6)
    public float modifyShade0(float value) {
        return veil$DEFERRED.get() ? 1.0F : value;
    }

    @ModifyArg(method = "renderModelFaceFlat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"), index = 7)
    public float modifyShade1(float value) {
        return veil$DEFERRED.get() ? 1.0F : value;
    }

    @ModifyArg(method = "renderModelFaceFlat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"), index = 8)
    public float modifyShade2(float value) {
        return veil$DEFERRED.get() ? 1.0F : value;
    }

    @ModifyArg(method = "renderModelFaceFlat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"), index = 9)
    public float modifyShade3(float value) {
        return veil$DEFERRED.get() ? 1.0F : value;
    }

    @ModifyVariable(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true, remap = false)
    public VertexConsumer modifyConsumer(VertexConsumer value) {
        return veil$DEFERRED.get() ? new DeferredVertexConsumer(value) : value;
    }
}
