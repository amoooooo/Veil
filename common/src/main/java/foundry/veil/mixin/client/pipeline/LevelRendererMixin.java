package foundry.veil.mixin.client.pipeline;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.CameraMatrices;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.ext.LevelRendererExtension;
import foundry.veil.impl.client.render.shader.VeilVanillaShaders;
import foundry.veil.api.compat.SodiumCompat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements LevelRendererExtension {

    @Shadow
    private Frustum cullingFrustum;

    @Shadow
    @Nullable
    private Frustum capturedFrustum;

    @Shadow
    protected abstract void renderSectionLayer(RenderType pRenderType, double pX, double pY, double pZ, Matrix4f pFrustrumMatrix, Matrix4f pProjectionMatrix);

    @Shadow
    private @Nullable PostChain transparencyChain;

    @Shadow
    private @Nullable RenderTarget translucentTarget;

    @Shadow
    private @Nullable RenderTarget itemEntityTarget;

    @Shadow
    private @Nullable RenderTarget particlesTarget;

    @Shadow
    private @Nullable RenderTarget weatherTarget;

    @Shadow
    private @Nullable RenderTarget cloudsTarget;

    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private final Matrix4f veil$tempFrustum = new Matrix4f();
    @Unique
    private final Matrix4f veil$tempProjection = new Matrix4f();
    @Unique
    private final Vector3d veil$tempCameraPos = new Vector3d();

    @Inject(method = "prepareCullFrustum", at = @At("HEAD"))
    public void veil$setupLevelCamera(Vec3 pos, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        CameraMatrices matrices = VeilRenderSystem.renderer().getCameraMatrices();
        matrices.update(projectionMatrix, frustumMatrix, this.veil$tempCameraPos.set(pos.x(), pos.y(), pos.z()), 0.05F, this.minecraft.gameRenderer.getDepthFar());
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    public void blit(CallbackInfo ci, @Local ProfilerFiller profiler) {
        VeilRenderSystem.blit(profiler);
    }

    // This sets the blend function for rain correctly
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V", shift = At.Shift.BEFORE))
    public void setRainBlend(CallbackInfo ci) {
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    // Add custom world border shader
    @ModifyArg(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"))
    public Supplier<ShaderInstance> setWorldBorderShader(Supplier<ShaderInstance> supplier) {
        return VeilVanillaShaders::getWorldborder;
    }

    @Inject(method = "deinitTransparency", at = @At("RETURN"))
    public void veil$deinitTransparency(CallbackInfo ci) {
        FramebufferManager framebufferManager = VeilRenderSystem.renderer().getFramebufferManager();
        framebufferManager.removeFramebuffer(VeilFramebuffers.TRANSLUCENT_TARGET);
        framebufferManager.removeFramebuffer(VeilFramebuffers.ITEM_ENTITY_TARGET);
        framebufferManager.removeFramebuffer(VeilFramebuffers.PARTICLES_TARGET);
        framebufferManager.removeFramebuffer(VeilFramebuffers.WEATHER_TARGET);
        framebufferManager.removeFramebuffer(VeilFramebuffers.CLOUDS_TARGET);
    }

    @Inject(method = "initTransparency", at = @At("RETURN"))
    public void veil$initTransparency(CallbackInfo ci) {
        if (this.transparencyChain == null) {
            return;
        }

        FramebufferManager framebufferManager = VeilRenderSystem.renderer().getFramebufferManager();
        framebufferManager.setFramebuffer(VeilFramebuffers.TRANSLUCENT_TARGET, VeilRenderBridge.wrap(this.translucentTarget));
        framebufferManager.setFramebuffer(VeilFramebuffers.ITEM_ENTITY_TARGET, VeilRenderBridge.wrap(this.itemEntityTarget));
        framebufferManager.setFramebuffer(VeilFramebuffers.PARTICLES_TARGET, VeilRenderBridge.wrap(this.particlesTarget));
        framebufferManager.setFramebuffer(VeilFramebuffers.WEATHER_TARGET, VeilRenderBridge.wrap(this.weatherTarget));
        framebufferManager.setFramebuffer(VeilFramebuffers.CLOUDS_TARGET, VeilRenderBridge.wrap(this.cloudsTarget));
    }

    @Override
    public CullFrustum veil$getCullFrustum() {
        return VeilRenderBridge.create(this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum);
    }

    @Override
    public void veil$drawBlockLayer(RenderType renderType, double x, double y, double z, Matrix4fc frustum, Matrix4fc projection) {
        this.renderSectionLayer(renderType, x, y, z, this.veil$tempFrustum.set(frustum), this.veil$tempProjection.set(projection));
    }

    @Override
    public void veil$markChunksDirty() {
        SodiumCompat sodiumCompat = SodiumCompat.INSTANCE;

        if (sodiumCompat != null) {
            sodiumCompat.markChunksDirty();
        } else {
            for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
                section.setDirty(false);
            }
        }
    }
}