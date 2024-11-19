package foundry.veil.mixin.client.pipeline;

import foundry.veil.api.client.render.CameraMatrices;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.ext.LevelRendererExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements LevelRendererExtension {

    @Shadow
    private Frustum cullingFrustum;

    @Shadow
    @Nullable
    private Frustum capturedFrustum;

    @Unique
    private final Matrix4f veil$tempFrustum = new Matrix4f();

    @Unique
    private final Matrix4f veil$tempProjection = new Matrix4f();

    @Shadow
    protected abstract void renderSectionLayer(RenderType pRenderType, double pX, double pY, double pZ, Matrix4f pFrustrumMatrix, Matrix4f pProjectionMatrix);

    @Unique
    private final Vector3f veil$tempCameraPos = new Vector3f();

    @Inject(method = "prepareCullFrustum", at = @At("HEAD"))
    public void veil$setupLevelCamera(Vec3 pos, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        CameraMatrices matrices = VeilRenderSystem.renderer().getCameraMatrices();
        matrices.update(projectionMatrix, frustumMatrix, this.veil$tempCameraPos.set(pos.x(), pos.y(), pos.z()), 0.05F, Minecraft.getInstance().gameRenderer.getDepthFar());
    }

    @Override
    public CullFrustum veil$getCullFrustum() {
        return VeilRenderBridge.create(this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum);
    }

    @Override
    public void veil$drawBlockLayer(RenderType renderType, double x, double y, double z, Matrix4fc frustum, Matrix4fc projection) {
        this.renderSectionLayer(renderType, x, y, z, this.veil$tempFrustum.set(frustum), this.veil$tempProjection.set(projection));
    }
}