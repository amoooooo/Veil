package foundry.veil;

import com.mojang.blaze3d.platform.InputConstants;
import foundry.veil.api.client.editor.EditorManager;
import foundry.veil.api.client.registry.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.registry.EmitterShapeRegistry;
import foundry.veil.api.quasar.registry.RenderStyleRegistry;
import foundry.veil.impl.client.editor.*;
import foundry.veil.impl.client.imgui.VeilImGuiImpl;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManger;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferShard;
import foundry.veil.impl.quasar.QuasarParticleHandler;
import foundry.veil.impl.resource.VeilResourceManagerImpl;
import foundry.veil.platform.VeilClientPlatform;
import foundry.veil.platform.VeilEventPlatform;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.ApiStatus;

import java.util.ServiceLoader;

public class VeilClient {

    private static final VeilClientPlatform PLATFORM = ServiceLoader.load(VeilClientPlatform.class).findFirst().orElseThrow(() -> new RuntimeException("Veil expected client platform implementation"));
    private static final VeilResourceManagerImpl RESOURCE_MANAGER = new VeilResourceManagerImpl();
    public static final KeyMapping EDITOR_KEY = new KeyMapping("key.veil.editor", InputConstants.Type.KEYSYM, InputConstants.KEY_F6, "key.categories.veil");

    @ApiStatus.Internal
    public static void init() {
        VeilImGuiImpl.setImGuiPath();
        QuasarParticleHandler.init();

        VeilEventPlatform.INSTANCE.onFreeNativeResources(() -> {
            VeilRenderSystem.close();
            RESOURCE_MANAGER.free();
        });
        VeilEventPlatform.INSTANCE.onVeilRendererAvailable(renderer -> {
            RESOURCE_MANAGER.addVeilLoaders(renderer);
            if (VeilRenderSystem.hasImGui()) {
                EditorManager editorManager = renderer.getEditorManager();

                // Example for devs
                if (Veil.platform().isDevelopmentEnvironment()) {
                    editorManager.add(new DemoInspector());
                }

                // Debug editors
                editorManager.add(new PostInspector());
                editorManager.add(new ShaderInspector());
                editorManager.add(new TextureInspector());
                editorManager.add(new OpenCLInspector());
                editorManager.add(new DeviceInfoViewer());
                editorManager.add(new LightInspector());
                editorManager.add(new FramebufferInspector());
                editorManager.add(new ResourceManagerInspector());
            }
//            glEnable(GL_DEPTH_CLAMP); // TODO add config option
        });

        // This fixes moving transparent blocks drawing too early
        VeilEventPlatform.INSTANCE.onVeilRegisterFixedBuffers(registry -> registry.registerFixedBuffer(VeilRenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS, RenderType.translucentMovingBlock()));
        RenderTypeShardRegistry.addGenericShard(renderType -> "main_target".equals(getOutputName(renderType)), new DynamicBufferShard(DynamicBufferManger.MAIN_WRAPPER, () -> Minecraft.getInstance().getMainRenderTarget()));
//        RenderTypeShardRegistry.addGenericShard(renderType -> "outline_target".equals(getOutputName(renderType)), new DynamicBufferShard("outline", () -> null));
        RenderTypeShardRegistry.addGenericShard(renderType -> "translucent_target".equals(getOutputName(renderType)), new DynamicBufferShard("translucent", () -> Minecraft.getInstance().levelRenderer.getTranslucentTarget()));
        RenderTypeShardRegistry.addGenericShard(renderType -> "particles_target".equals(getOutputName(renderType)), new DynamicBufferShard("particles", () -> Minecraft.getInstance().levelRenderer.getParticlesTarget()));
        RenderTypeShardRegistry.addGenericShard(renderType -> "weather_target".equals(getOutputName(renderType)), new DynamicBufferShard("weather", () -> Minecraft.getInstance().levelRenderer.getWeatherTarget()));
        RenderTypeShardRegistry.addGenericShard(renderType -> "clouds_target".equals(getOutputName(renderType)), new DynamicBufferShard("clouds", () -> Minecraft.getInstance().levelRenderer.getCloudsTarget()));
        RenderTypeShardRegistry.addGenericShard(renderType -> "item_entity_target".equals(getOutputName(renderType)), new DynamicBufferShard("item_entity", () -> Minecraft.getInstance().levelRenderer.getItemEntityTarget()));
        PostPipelineStageRegistry.bootstrap();
        LightTypeRegistry.bootstrap();
        RenderTypeLayerRegistry.bootstrap();
        VeilShaderBufferRegistry.bootstrap();
        VeilResourceEditorRegistry.bootstrap();
        EmitterShapeRegistry.bootstrap();
        RenderStyleRegistry.bootstrap();
        ParticleModuleTypeRegistry.bootstrap();
    }

    private static String getOutputName(RenderType.CompositeRenderType renderType) {
        return VeilRenderType.getName(VeilRenderType.getShards(renderType).outputState());
    }

    @ApiStatus.Internal
    public static void initRenderer() {
        VeilRenderSystem.init();
    }

    @ApiStatus.Internal
    public static void tickClient(float partialTick) {
    }

    public static VeilClientPlatform clientPlatform() {
        return PLATFORM;
    }

    public static VeilResourceManagerImpl resourceManager() {
        return RESOURCE_MANAGER;
    }
}
