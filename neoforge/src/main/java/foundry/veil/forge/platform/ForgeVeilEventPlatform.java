package foundry.veil.forge.platform;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.event.*;
import foundry.veil.forge.event.*;
import foundry.veil.platform.VeilEventPlatform;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;

import java.util.Map;

@ApiStatus.Internal
public class ForgeVeilEventPlatform implements VeilEventPlatform {

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

    @Override
    public void onFreeNativeResources(FreeNativeResourcesEvent event) {
        MinecraftForge.EVENT_BUS.<ForgeFreeNativeResourcesEvent>addListener(forgeEvent -> event.onFree());
    }

    @Override
    public void onVeilRendererAvailable(VeilRendererEvent event) {
        FMLJavaModLoadingContext.get().getModEventBus().<ForgeVeilRendererEvent>addListener(forgeEvent -> event.onVeilRendererAvailable(forgeEvent.getRenderer()));
    }

    @Override
    public void preVeilPostProcessing(VeilPostProcessingEvent.Pre event) {
        MinecraftForge.EVENT_BUS.<ForgeVeilPostProcessingEvent.Pre>addListener(forgeEvent -> event.preVeilPostProcessing(forgeEvent.getName(), forgeEvent.getPipeline(), forgeEvent.getContext()));
    }

    @Override
    public void postVeilPostProcessing(VeilPostProcessingEvent.Post event) {
        MinecraftForge.EVENT_BUS.<ForgeVeilPostProcessingEvent.Post>addListener(forgeEvent -> event.postVeilPostProcessing(forgeEvent.getName(), forgeEvent.getPipeline(), forgeEvent.getContext()));
    }

    @Override
    public void onVeilRegisterFixedBuffers(VeilRegisterFixedBuffersEvent event) {
        FMLJavaModLoadingContext.get().getModEventBus().<ForgeVeilRegisterFixedBuffersEvent>addListener(forgeEvent -> event.onRegisterFixedBuffers((stage, renderType) -> {
            if (stage == null) {
                forgeEvent.register(null, renderType);
                return;
            }

            RenderLevelStageEvent.Stage forgeStage = STAGE_MAPPING.get(stage);
            if (forgeStage != null) {
                forgeEvent.register(forgeStage, renderType);
            }
        }));
    }

    @Override
    public void onVeilRegisterBlockLayers(VeilRegisterBlockLayerEvent event) {
        FMLJavaModLoadingContext.get().getModEventBus().<ForgeVeilRegisterBlockLayerEvent>addListener(event::onRegisterBlockLayers);
    }

    @Override
    public void onVeilRenderTypeStageRender(VeilRenderLevelStageEvent event) {
        MinecraftForge.EVENT_BUS.<RenderLevelStageEvent>addListener(forgeEvent -> {
            VeilRenderLevelStageEvent.Stage stage = STAGE_MAPPING.inverse().get(forgeEvent.getStage());
            if (stage == null) {
                return;
            }

            LevelRenderer levelRenderer = forgeEvent.getLevelRenderer();
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            PoseStack poseStack = forgeEvent.getPoseStack();
            Matrix4f projectionMatrix = forgeEvent.getProjectionMatrix();
            int renderTick = forgeEvent.getRenderTick();
            float partialTicks = forgeEvent.getPartialTick();
            Camera camera = forgeEvent.getCamera();
            Frustum frustum = forgeEvent.getFrustum();
            event.onRenderLevelStage(stage, levelRenderer, bufferSource, poseStack, projectionMatrix, renderTick, partialTicks, camera, frustum);
        });
    }
}
