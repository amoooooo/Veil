package foundry.veil.mixin.pipeline.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.CameraMatrices;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.compat.SodiumCompat;
import foundry.veil.ext.LevelRendererExtension;
import foundry.veil.impl.client.render.shader.VeilVanillaShaders;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
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

import java.util.List;
import java.util.function.Supplier;

@Mixin(LevelRenderer.class)
public abstract class PipelineLevelRendererMixin implements LevelRendererExtension {

    @Shadow
    private Frustum cullingFrustum;

    @Shadow
    @Nullable
    private Frustum capturedFrustum;

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

    @Shadow
    protected abstract void renderSectionLayer(RenderType pRenderType, double pX, double pY, double pZ, Matrix4f pFrustrumMatrix, Matrix4f pProjectionMatrix);

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
        if (VeilRenderSystem.drawLights(profiler, VeilRenderSystem.getCullingFrustum())) {
            VeilRenderSystem.compositeLights(profiler);
        } else {
            AdvancedFbo.unbind();
        }
    }

    // This sets the blend function for rain correctly
    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=weather"))
    public void setRainBlend(CallbackInfo ci) {
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    // Add custom world border shader
    @ModifyArg(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V", remap = false))
    public Supplier<ShaderInstance> setWorldBorderShader(Supplier<ShaderInstance> supplier) {
        return VeilVanillaShaders::getWorldborder;
    }

    @Inject(method = "deinitTransparency", at = @At("RETURN"))
    public void deinitTransparency(CallbackInfo ci) {
        FramebufferManager framebufferManager = VeilRenderSystem.renderer().getFramebufferManager();
        framebufferManager.removeFramebuffer(VeilFramebuffers.TRANSLUCENT_TARGET);
        framebufferManager.removeFramebuffer(VeilFramebuffers.ITEM_ENTITY_TARGET);
        framebufferManager.removeFramebuffer(VeilFramebuffers.PARTICLES_TARGET);
        framebufferManager.removeFramebuffer(VeilFramebuffers.WEATHER_TARGET);
        framebufferManager.removeFramebuffer(VeilFramebuffers.CLOUDS_TARGET);
    }

    @Inject(method = "initTransparency", at = @At("RETURN"))
    public void initTransparency(CallbackInfo ci) {
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

    @Inject(method = "setLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;waitAndReset(Lnet/minecraft/client/renderer/ViewArea;)V"))
    public void free(ClientLevel level, CallbackInfo ci) {
        VeilRenderSystem.clearLevel();
    }

    @Override
    public CullFrustum veil$getCullFrustum() {
        return VeilRenderBridge.create(this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum);
    }

    @Override
    public void veil$drawBlockLayer(RenderType renderType, double x, double y, double z, Matrix4fc frustum, Matrix4fc projection) {
        RenderSystem.assertOnRenderThread();

        if (renderType instanceof VeilRenderType.LayeredRenderType layeredRenderType) {
            ProfilerFiller profiler = this.minecraft.getProfiler();

            this.veil$tempFrustum.set(frustum);
            this.veil$tempProjection.set(projection);
            Window window = this.minecraft.getWindow();

            List<RenderType> layers = layeredRenderType.getLayers();
            boolean rendered = false;

            profiler.push("render_" + VeilRenderType.getName(renderType));
            boolean forward = !renderType.sortOnUpload();
            ObjectListIterator<SectionRenderDispatcher.RenderSection> objectlistiterator = this.visibleSections.listIterator(forward ? 0 : this.visibleSections.size());

            ShaderInstance shaderInstance = null;
            Uniform chunkOffset = null;

            ObjectList<SectionRenderDispatcher.RenderSection> validSections = new ObjectArrayList<>(this.visibleSections.size());
            ObjectList<VertexBuffer> buffers = new ObjectArrayList<>(this.visibleSections.size());
            while (true) {
                if (forward) {
                    if (!objectlistiterator.hasNext()) {
                        break;
                    }
                } else if (!objectlistiterator.hasPrevious()) {
                    break;
                }

                SectionRenderDispatcher.RenderSection section = forward ? objectlistiterator.next() : objectlistiterator.previous();
                if (!section.getCompiled().isEmpty(renderType)) {
                    // Don't set up the render state until something is actually rendered
                    if (!rendered) {
                        renderType.setupRenderState();
                        shaderInstance = RenderSystem.getShader();
                        if (shaderInstance != null) {
                            shaderInstance.setDefaultUniforms(VertexFormat.Mode.QUADS, this.veil$tempFrustum, this.veil$tempProjection, window);
                            shaderInstance.apply();
                            chunkOffset = shaderInstance.CHUNK_OFFSET;
                        }
                        rendered = true;
                    }

                    if (chunkOffset != null) {
                        BlockPos origin = section.getOrigin();
                        chunkOffset.set((float) (origin.getX() - x), (float) (origin.getY() - y), (float) (origin.getZ() - z));
                        chunkOffset.upload();
                    }

                    VertexBuffer vertexbuffer = section.getBuffer(renderType);
                    vertexbuffer.bind();
                    vertexbuffer.draw();

                    // Keep track of valid sections, so we can loop immediately after
                    validSections.add(section);
                    buffers.add(vertexbuffer);
                }
            }

            if (!rendered) {
                profiler.pop();
                return;
            }

            if (chunkOffset != null) {
                chunkOffset.set(0.0F, 0.0F, 0.0F);
            }
            if (shaderInstance != null) {
                shaderInstance.clear();
            }
            renderType.clearRenderState();
            profiler.pop();

            if (!validSections.isEmpty()) {
                // Loop again to draw each layer, making sure not to loop through EVERY section
                for (RenderType layer : layers) {
                    layer.setupRenderState();
                    profiler.push("render_" + VeilRenderType.getName(layers.getFirst()));
                    shaderInstance = RenderSystem.getShader();
                    if (shaderInstance != null) {
                        shaderInstance.setDefaultUniforms(VertexFormat.Mode.QUADS, this.veil$tempFrustum, this.veil$tempProjection, window);
                        shaderInstance.apply();
                        chunkOffset = shaderInstance.CHUNK_OFFSET;
                    }

                    for (int j = 0; j < validSections.size(); j++) {
                        SectionRenderDispatcher.RenderSection section = validSections.get(j);
                        VertexBuffer vertexbuffer = buffers.get(j);

                        if (chunkOffset != null) {
                            BlockPos origin = section.getOrigin();
                            chunkOffset.set((float) (origin.getX() - x), (float) (origin.getY() - y), (float) (origin.getZ() - z));
                            chunkOffset.upload();
                        }

                        vertexbuffer.bind();
                        vertexbuffer.draw();
                    }

                    if (chunkOffset != null) {
                        chunkOffset.set(0.0F, 0.0F, 0.0F);
                    }
                    if (shaderInstance != null) {
                        shaderInstance.clear();
                    }
                    profiler.pop();
                    layer.clearRenderState();
                }
            }

            VertexBuffer.unbind();
        } else {
            this.renderSectionLayer(renderType, x, y, z, this.veil$tempFrustum.set(frustum), this.veil$tempProjection.set(projection));
        }
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