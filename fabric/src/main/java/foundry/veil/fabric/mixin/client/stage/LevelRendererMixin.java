package foundry.veil.fabric.mixin.client.stage;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.ext.LevelRendererBlockLayerExtension;
import foundry.veil.fabric.FabricRenderTypeStageHandler;
import foundry.veil.fabric.ext.LevelRendererExtension;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin implements LevelRendererExtension {

    @Shadow
    private @Nullable ClientLevel level;

    @Shadow
    private int ticks;

    @Shadow
    private @Nullable Frustum capturedFrustum;

    @Shadow
    private Frustum cullingFrustum;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Unique
    private DeltaTracker veil$captureDeltaTracker;
    @Unique
    private Camera veil$captureCamera;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void capture(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        this.veil$captureDeltaTracker = deltaTracker;
        this.veil$captureCamera = camera;
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    public void clear(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        this.veil$captureCamera = null;
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", shift = At.Shift.AFTER))
    public void postRenderSky(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, @Local ProfilerFiller profiler, @Local Frustum frustum) {
        FabricRenderTypeStageHandler.renderStage((LevelRendererBlockLayerExtension) this, profiler, VeilRenderLevelStageEvent.Stage.AFTER_SKY, (LevelRenderer) (Object) this, this.renderBuffers.bufferSource(), null, frustumMatrix, projectionMatrix, this.ticks, deltaTracker, camera, frustum);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch(Lnet/minecraft/client/renderer/RenderType;)V", ordinal = 3, shift = At.Shift.AFTER))
    public void postRenderEntities(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, @Local ProfilerFiller profiler, @Local Frustum frustum, @Local PoseStack poseStack) {
        FabricRenderTypeStageHandler.renderStage((LevelRendererBlockLayerExtension) this, profiler, VeilRenderLevelStageEvent.Stage.AFTER_ENTITIES, (LevelRenderer) (Object) this, this.renderBuffers.bufferSource(), poseStack, frustumMatrix, projectionMatrix, this.ticks, deltaTracker, camera, frustum);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;long2ObjectEntrySet()Lit/unimi/dsi/fastutil/objects/ObjectSet;", shift = At.Shift.BEFORE))
    public void postRenderBlockEntities(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, @Local ProfilerFiller profiler, @Local Frustum frustum, @Local PoseStack poseStack) {
        FabricRenderTypeStageHandler.renderStage((LevelRendererBlockLayerExtension) this, profiler, VeilRenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES, (LevelRenderer) (Object) this, this.renderBuffers.bufferSource(), poseStack, frustumMatrix, projectionMatrix, this.ticks, deltaTracker, camera, frustum);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void postRenderParticles(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, @Local ProfilerFiller profiler, @Local Frustum frustum, @Local PoseStack poseStack) {
        FabricRenderTypeStageHandler.renderStage((LevelRendererBlockLayerExtension) this, profiler, VeilRenderLevelStageEvent.Stage.AFTER_PARTICLES, (LevelRenderer) (Object) this, this.renderBuffers.bufferSource(), poseStack, frustumMatrix, projectionMatrix, this.ticks, deltaTracker, camera, frustum);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void postRenderWeather(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, @Local ProfilerFiller profiler, @Local Frustum frustum, @Local PoseStack poseStack) {
        FabricRenderTypeStageHandler.renderStage((LevelRendererBlockLayerExtension) this, profiler, VeilRenderLevelStageEvent.Stage.AFTER_WEATHER, (LevelRenderer) (Object) this, this.renderBuffers.bufferSource(), poseStack, frustumMatrix, projectionMatrix, this.ticks, deltaTracker, camera, frustum);
    }

    @Inject(method = "renderLevel", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void postRenderLevel(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, @Local ProfilerFiller profiler, @Local Frustum frustum, @Local PoseStack poseStack) {
        FabricRenderTypeStageHandler.renderStage((LevelRendererBlockLayerExtension) this, profiler, VeilRenderLevelStageEvent.Stage.AFTER_LEVEL, (LevelRenderer) (Object) this, this.renderBuffers.bufferSource(), poseStack, frustumMatrix, projectionMatrix, this.ticks, deltaTracker, camera, frustum);
    }

    @Override
    public void veil$renderStage(RenderType layer, Matrix4fc frustumMatrix, Matrix4fc projection) {
        VeilRenderLevelStageEvent.Stage stage;
        if (layer == RenderType.solid()) {
            stage = VeilRenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS;
        } else if (layer == RenderType.cutoutMipped()) {
            stage = VeilRenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS;
        } else if (layer == RenderType.cutout()) {
            stage = VeilRenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS;
        } else if (layer == RenderType.translucent()) {
            stage = VeilRenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
        } else if (layer == RenderType.tripwire()) {
            stage = VeilRenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS;
        } else {
            stage = null;
        }

        if (stage != null) {
            FabricRenderTypeStageHandler.renderStage((LevelRendererBlockLayerExtension) this, this.level.getProfiler(), stage, (LevelRenderer) (Object) this, this.renderBuffers.bufferSource(), null, frustumMatrix, projection, this.ticks, this.veil$captureDeltaTracker, this.veil$captureCamera, this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum);
        }
    }
}
