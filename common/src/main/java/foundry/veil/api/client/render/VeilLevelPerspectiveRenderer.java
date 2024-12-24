package foundry.veil.api.client.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.ext.RenderTargetExtension;
import foundry.veil.impl.client.render.perspective.LevelPerspectiveCamera;
import foundry.veil.mixin.accessor.GameRendererAccessor;
import foundry.veil.mixin.accessor.LevelRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import static org.lwjgl.opengl.GL11C.glDrawBuffer;
import static org.lwjgl.opengl.GL20C.glDrawBuffers;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;

/**
 * Renders the level from different perspectives.
 *
 * @author Ocelot
 */
public final class VeilLevelPerspectiveRenderer {

    private static final LevelPerspectiveCamera CAMERA = new LevelPerspectiveCamera();
    private static final Matrix4f TRANSFORM = new Matrix4f();

    private static final Matrix4f BACKUP_PROJECTION = new Matrix4f();
    private static final Vector3f BACKUP_LIGHT0_POSITION = new Vector3f();
    private static final Vector3f BACKUP_LIGHT1_POSITION = new Vector3f();

    private static boolean renderingPerspective = false;

    private VeilLevelPerspectiveRenderer() {
    }

    /**
     * Renders the level from another POV. Automatically prevents circular render references.
     *
     * @param framebuffer       The framebuffer to draw into
     * @param modelView         The base modelview matrix
     * @param projection        The projection matrix
     * @param cameraPosition    The position of the camera
     * @param cameraOrientation The orientation of the camera
     * @param renderDistance    The chunk render distance
     * @param deltaTracker      The delta tracker instance
     */
    public static void render(AdvancedFbo framebuffer, Matrix4fc modelView, Matrix4fc projection, Vector3dc cameraPosition, Quaternionfc cameraOrientation, float renderDistance, DeltaTracker deltaTracker) {
        render(framebuffer, Minecraft.getInstance().cameraEntity, modelView, projection, cameraPosition, cameraOrientation, renderDistance, deltaTracker);
    }

    /**
     * Renders the level from another POV. Automatically prevents circular render references.
     *
     * @param framebuffer       The framebuffer to draw into
     * @param cameraEntity      The entity to draw the camera in relation to. If unsure use {@link #render(AdvancedFbo, Matrix4fc, Matrix4fc, Vector3dc, Quaternionfc, float, DeltaTracker)}
     * @param modelView         The base modelview matrix
     * @param projection        The projection matrix
     * @param cameraPosition    The position of the camera
     * @param cameraOrientation The orientation of the camera
     * @param renderDistance    The chunk render distance
     * @param deltaTracker      The delta tracker instance
     */
    public static void render(AdvancedFbo framebuffer, @Nullable Entity cameraEntity, Matrix4fc modelView, Matrix4fc projection, Vector3dc cameraPosition, Quaternionfc cameraOrientation, float renderDistance, DeltaTracker deltaTracker) {
        if (renderingPerspective) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final GameRenderer gameRenderer = minecraft.gameRenderer;
        final LevelRenderer levelRenderer = minecraft.levelRenderer;
        final LevelRendererAccessor levelRendererAccessor = (LevelRendererAccessor) levelRenderer;
        final Window window = minecraft.getWindow();
        final GameRendererAccessor accessor = (GameRendererAccessor) gameRenderer;
        final RenderTargetExtension renderTargetExtension = (RenderTargetExtension) minecraft.getMainRenderTarget();
        final PoseStack poseStack = new PoseStack();

        CAMERA.setup(cameraPosition, cameraEntity, minecraft.level, cameraOrientation, renderDistance);

        poseStack.mulPose(TRANSFORM.set(modelView));
        poseStack.mulPose(CAMERA.rotation());

        float backupRenderDistance = gameRenderer.getRenderDistance();
        accessor.setRenderDistance(renderDistance * 16.0F);

        float backupFogStart = RenderSystem.getShaderFogStart();
        float backupFogEnd = RenderSystem.getShaderFogEnd();
        FogShape backupFogShape = RenderSystem.getShaderFogShape();

        int backupWidth = window.getWidth();
        int backupHeight = window.getHeight();
        window.setWidth(framebuffer.getWidth());
        window.setHeight(framebuffer.getHeight());

        BACKUP_PROJECTION.set(RenderSystem.getProjectionMatrix());
        gameRenderer.resetProjectionMatrix(TRANSFORM.set(projection));
        BACKUP_LIGHT0_POSITION.set(VeilRenderSystem.getLight0Direction());
        BACKUP_LIGHT1_POSITION.set(VeilRenderSystem.getLight1Direction());

        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.identity();
        RenderSystem.applyModelViewMatrix();

        HitResult backupHitResult = minecraft.hitResult;
        Entity backupCrosshairPickEntity = minecraft.crosshairPickEntity;

        renderingPerspective = true;
        AdvancedFbo drawFbo = VeilRenderSystem.renderer().getDynamicBufferManger().getDynamicFbo(framebuffer, true);
        if (drawFbo != null) {
            drawFbo.bindDraw(true);
            renderTargetExtension.veil$setWrapper(drawFbo);
        } else {
            framebuffer.bindDraw(true);
            renderTargetExtension.veil$setWrapper(framebuffer);
        }

        Frustum backupFrustum = levelRendererAccessor.getCullingFrustum();

        levelRenderer.prepareCullFrustum(new Vec3(cameraPosition.x(), cameraPosition.y(), cameraPosition.z()), poseStack.last().pose(), TRANSFORM);
        levelRenderer.renderLevel(deltaTracker, false, CAMERA, gameRenderer, gameRenderer.lightTexture(), poseStack.last().pose(), TRANSFORM);
        levelRenderer.doEntityOutline();

        levelRendererAccessor.setCullingFrustum(backupFrustum);

        renderTargetExtension.veil$setWrapper(null);
        if (drawFbo != null) {
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            drawFbo.resolveToAdvancedFbo(framebuffer);
            drawFbo.bind(false);
            glDrawBuffers(drawFbo.getDrawBuffers());
        }
        AdvancedFbo.unbind();
        renderingPerspective = false;

        minecraft.crosshairPickEntity = backupCrosshairPickEntity;
        minecraft.hitResult = backupHitResult;

        matrix4fstack.popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setShaderLights(BACKUP_LIGHT0_POSITION, BACKUP_LIGHT1_POSITION);
        gameRenderer.resetProjectionMatrix(BACKUP_PROJECTION);

        RenderSystem.setShaderFogStart(backupFogStart);
        RenderSystem.setShaderFogEnd(backupFogEnd);
        RenderSystem.setShaderFogShape(backupFogShape);

        window.setWidth(backupWidth);
        window.setHeight(backupHeight);

        accessor.setRenderDistance(backupRenderDistance);

        // Reset the renderers to what they used to be
        Camera mainCamera = gameRenderer.getMainCamera();
        minecraft.getBlockEntityRenderDispatcher().prepare(minecraft.level, mainCamera, minecraft.hitResult);
        minecraft.getEntityRenderDispatcher().prepare(minecraft.level, mainCamera, minecraft.crosshairPickEntity);
    }

    /**
     * @return Whether a perspective is being rendered
     */
    public static boolean isRenderingPerspective() {
        return renderingPerspective;
    }
}
