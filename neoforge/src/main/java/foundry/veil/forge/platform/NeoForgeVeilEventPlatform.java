package foundry.veil.forge.platform;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.event.*;
import foundry.veil.forge.event.*;
import foundry.veil.platform.VeilEventPlatform;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Map;

@ApiStatus.Internal
public class NeoForgeVeilEventPlatform implements VeilEventPlatform {

    private static final BiMap<VeilRenderLevelStageEvent.Stage, RenderLevelStageEvent.Stage> STAGE_MAPPING = HashBiMap.create(Map.ofEntries(
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_SKY, RenderLevelStageEvent.Stage.AFTER_SKY),
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS, RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS),
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS, RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS),
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS, RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS),
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_ENTITIES, RenderLevelStageEvent.Stage.AFTER_ENTITIES),
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES, RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES),
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS, RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS),
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS, RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS),
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_PARTICLES, RenderLevelStageEvent.Stage.AFTER_PARTICLES),
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_WEATHER, RenderLevelStageEvent.Stage.AFTER_WEATHER),
            Map.entry(VeilRenderLevelStageEvent.Stage.AFTER_LEVEL, RenderLevelStageEvent.Stage.AFTER_LEVEL)
    ));

    private IEventBus getModBus() {
        ModContainer container = ModLoadingContext.get().getActiveContainer();
        if (container.getEventBus() == null) {
            throw new IllegalStateException("Veil platform events must be registered from mod constructor");
        }
        return container.getEventBus();
    }

    @Override
    public void onFreeNativeResources(FreeNativeResourcesEvent event) {
        NeoForge.EVENT_BUS.<ForgeFreeNativeResourcesEvent>addListener(forgeEvent -> event.onFree());
    }

    @Override
    public void onVeilAddShaderProcessors(VeilAddShaderPreProcessorsEvent event) {
        this.getModBus().<ForgeVeilAddShaderProcessorsEvent>addListener(forgeEvent -> event.onRegisterShaderPreProcessors(forgeEvent.getResourceProvider(), forgeEvent));
    }

    @Override
    public void preVeilPostProcessing(VeilPostProcessingEvent.Pre event) {
        NeoForge.EVENT_BUS.<ForgeVeilPostProcessingEvent.Pre>addListener(forgeEvent -> event.preVeilPostProcessing(forgeEvent.getName(), forgeEvent.getPipeline(), forgeEvent.getContext()));
    }

    @Override
    public void postVeilPostProcessing(VeilPostProcessingEvent.Post event) {
        NeoForge.EVENT_BUS.<ForgeVeilPostProcessingEvent.Post>addListener(forgeEvent -> event.postVeilPostProcessing(forgeEvent.getName(), forgeEvent.getPipeline(), forgeEvent.getContext()));
    }

    @Override
    public void onVeilRegisterBlockLayers(VeilRegisterBlockLayersEvent event) {
        this.getModBus().<ForgeVeilRegisterBlockLayersEvent>addListener(event::onRegisterBlockLayers);
    }

    @Override
    public void onVeilRegisterFixedBuffers(VeilRegisterFixedBuffersEvent event) {
        this.getModBus().<ForgeVeilRegisterFixedBuffersEvent>addListener(forgeEvent -> event.onRegisterFixedBuffers((stage, renderType) -> {
            if (stage == null) {
                forgeEvent.register(null, renderType);
                return;
            }

            RenderLevelStageEvent.Stage forgeStage = getForgeStage(stage);
            if (forgeStage != null) {
                forgeEvent.register(forgeStage, renderType);
            }
        }));
    }

    @Override
    public void onVeilRendererAvailable(VeilRendererAvailableEvent event) {
        this.getModBus().<ForgeVeilRendererAvailableEvent>addListener(forgeEvent -> event.onVeilRendererAvailable(forgeEvent.getRenderer()));
    }

    @Override
    public void onVeilRenderLevelStage(VeilRenderLevelStageEvent event) {
        NeoForge.EVENT_BUS.<RenderLevelStageEvent>addListener(forgeEvent -> {
            VeilRenderLevelStageEvent.Stage stage = getVeilStage(forgeEvent.getStage());
            if (stage == null) {
                return;
            }

            LevelRenderer levelRenderer = forgeEvent.getLevelRenderer();
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            MatrixStack poseStack = VeilRenderBridge.create(forgeEvent.getPoseStack());
            Matrix4f modelViewMatrix = forgeEvent.getModelViewMatrix();
            Matrix4f projectionMatrix = forgeEvent.getProjectionMatrix();
            int renderTick = forgeEvent.getRenderTick();
            DeltaTracker deltaTracker = forgeEvent.getPartialTick();
            Camera camera = forgeEvent.getCamera();
            Frustum frustum = forgeEvent.getFrustum();
            event.onRenderLevelStage(stage, levelRenderer, bufferSource, poseStack, modelViewMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum);
        });
    }

    @Override
    public void onVeilShaderCompile(VeilShaderCompileEvent event) {
        this.getModBus().<ForgeVeilShaderCompileEvent>addListener(forgeEvent -> event.onVeilCompileShaders(forgeEvent.getShaderManager(), forgeEvent.getUpdatedPrograms()));
    }

    public static @Nullable RenderLevelStageEvent.Stage getForgeStage(VeilRenderLevelStageEvent.Stage stage) {
        return STAGE_MAPPING.get(stage);
    }

    public static @Nullable VeilRenderLevelStageEvent.Stage getVeilStage(RenderLevelStageEvent.Stage stage) {
        return STAGE_MAPPING.inverse().get(stage);
    }
}
