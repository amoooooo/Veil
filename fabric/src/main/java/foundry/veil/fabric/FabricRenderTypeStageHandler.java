package foundry.veil.fabric;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.ext.LevelRendererBlockLayerExtension;
import foundry.veil.fabric.event.FabricVeilRenderLevelStageEvent;
import foundry.veil.mixin.rendertype.accessor.RenderTypeBufferSourceAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import java.util.*;

@ApiStatus.Internal
public class FabricRenderTypeStageHandler {

    private static final Map<VeilRenderLevelStageEvent.Stage, Set<RenderType>> STAGE_RENDER_TYPES = new HashMap<>();
    private static Set<RenderType> CUSTOM_BLOCK_LAYERS;
    private static List<RenderType> BLOCK_LAYERS;

    public static void register(@Nullable VeilRenderLevelStageEvent.Stage stage, RenderType renderType) {
        SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers = ((RenderTypeBufferSourceAccessor) Minecraft.getInstance().renderBuffers().bufferSource()).getFixedBuffers();
        ByteBufferBuilder old = fixedBuffers.put(renderType, new ByteBufferBuilder(renderType.bufferSize()));
        if (old != null) {
            old.close();
        }

        if (stage != null) {
            STAGE_RENDER_TYPES.computeIfAbsent(stage, unused -> new HashSet<>()).add(renderType);
        }
    }

    public static void renderStage(LevelRendererBlockLayerExtension extension, ProfilerFiller profiler, VeilRenderLevelStageEvent.Stage stage, LevelRenderer levelRenderer, MultiBufferSource.BufferSource bufferSource, @Nullable PoseStack poseStack, Matrix4fc frustumMatrix, Matrix4fc projectionMatrix, int renderTick, DeltaTracker deltaTracker, Camera camera, Frustum frustum) {
        profiler.push(stage.getName());
        FabricVeilRenderLevelStageEvent.EVENT.invoker().onRenderLevelStage(stage, levelRenderer, bufferSource, VeilRenderBridge.create(poseStack != null ? poseStack : new PoseStack()), frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum);
        profiler.popPush("post");
        if (!VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            VeilRenderSystem.renderPost(stage);
        }
        profiler.pop();

        Set<RenderType> stages = STAGE_RENDER_TYPES.get(stage);
        if (stages != null) {
            stages.forEach(renderType -> {
                if (CUSTOM_BLOCK_LAYERS.contains(renderType)) {
                    Vec3 pos = camera.getPosition();
                    extension.veil$drawBlockLayer(renderType, pos.x, pos.y, pos.z, frustumMatrix, projectionMatrix);
                }
                bufferSource.endBatch(renderType);
            });
        }
    }

    public static List<RenderType> getBlockLayers() {
        return BLOCK_LAYERS;
    }

    public static void setBlockLayers(ImmutableList.Builder<RenderType> blockLayers) {
        CUSTOM_BLOCK_LAYERS = new HashSet<>(blockLayers.build());
        blockLayers.addAll(RenderType.chunkBufferLayers());
        BLOCK_LAYERS = blockLayers.build();
    }
}
