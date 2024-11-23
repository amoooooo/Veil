package foundry.veil;

import com.mojang.blaze3d.platform.InputConstants;
import foundry.veil.api.client.editor.EditorManager;
import foundry.veil.api.client.registry.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.api.quasar.data.ParticleModuleTypeRegistry;
import foundry.veil.api.quasar.registry.EmitterShapeRegistry;
import foundry.veil.api.quasar.registry.RenderStyleRegistry;
import foundry.veil.impl.client.editor.*;
import foundry.veil.impl.client.imgui.VeilImGuiImpl;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManger;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferShard;
import foundry.veil.impl.resource.VeilResourceManagerImpl;
import foundry.veil.mixin.accessor.CompositeStateAccessor;
import foundry.veil.mixin.accessor.RenderStateShardAccessor;
import foundry.veil.platform.VeilClientPlatform;
import foundry.veil.platform.VeilEventPlatform;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.glfw.GLFW;

import java.util.ServiceLoader;

import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL32C.GL_DEPTH_CLAMP;

public class VeilClient {

    private static final VeilClientPlatform PLATFORM = ServiceLoader.load(VeilClientPlatform.class).findFirst().orElseThrow(() -> new RuntimeException("Veil expected client platform implementation"));
    private static final VeilResourceManagerImpl RESOURCE_MANAGER = new VeilResourceManagerImpl();
    public static final SystemToast.SystemToastId UNSUPPORTED_NOTIFICATION = new SystemToast.SystemToastId();
    public static final KeyMapping EDITOR_KEY = new KeyMapping("key.veil.editor", InputConstants.Type.KEYSYM, InputConstants.KEY_F6, "key.categories.veil");

    @ApiStatus.Internal
    public static void init() {
        VeilImGuiImpl.setImGuiPath();

        VeilEventPlatform.INSTANCE.onFreeNativeResources(() -> {
            VeilRenderSystem.close();
            RESOURCE_MANAGER.free();
        });
        VeilEventPlatform.INSTANCE.onVeilRendererAvailable(renderer -> {
//            if (Veil.SODIUM) {
//                SystemToast.add(Minecraft.getInstance().getToasts(), UNSUPPORTED_NOTIFICATION, VeilDeferredRenderer.UNSUPPORTED_TITLE, VeilDeferredRenderer.UNSUPPORTED_SODIUM_DESC);
//            }

            RESOURCE_MANAGER.addVeilLoaders(renderer);
            if (VeilRenderer.hasImGui()) {
                EditorManager editorManager = renderer.getEditorManager();

                // Example for devs
                if (Veil.platform().isDevelopmentEnvironment()) {
                    editorManager.add(new DemoEditor());
                }

                // Debug editors
                editorManager.add(new PostEditor());
                if (Veil.DEBUG) {
                    editorManager.add(new ShaderEditor());
                }
                editorManager.add(new TextureEditor());
                editorManager.add(new OpenCLEditor());
                editorManager.add(new DeviceInfoViewer());
//                editorManager.add(new DeferredEditor());
                editorManager.add(new LightEditor());
                editorManager.add(new FramebufferEditor());
                editorManager.add(new ResourceManagerEditor());
            }
            glEnable(GL_DEPTH_CLAMP); // TODO add config option
        });

        // This fixes moving transparent blocks drawing too early
        VeilEventPlatform.INSTANCE.onVeilRegisterFixedBuffers(registry -> registry.registerFixedBuffer(VeilRenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS, RenderType.translucentMovingBlock()));
        RenderTypeStageRegistry.addGenericStage(renderType -> "main_target".equals(getOutputName(renderType)), new DynamicBufferShard(DynamicBufferManger.MAIN_WRAPPER, () -> Minecraft.getInstance().getMainRenderTarget()));
        RenderTypeStageRegistry.addGenericStage(renderType -> "outline_target".equals(getOutputName(renderType)), new DynamicBufferShard("outline", () -> null));
        RenderTypeStageRegistry.addGenericStage(renderType -> "translucent_target".equals(getOutputName(renderType)), new DynamicBufferShard("translucent", () -> Minecraft.getInstance().levelRenderer.getTranslucentTarget()));
        RenderTypeStageRegistry.addGenericStage(renderType -> "particles_target".equals(getOutputName(renderType)), new DynamicBufferShard("particles", () -> Minecraft.getInstance().levelRenderer.getParticlesTarget()));
        RenderTypeStageRegistry.addGenericStage(renderType -> "weather_target".equals(getOutputName(renderType)), new DynamicBufferShard("weather", () -> Minecraft.getInstance().levelRenderer.getWeatherTarget()));
        RenderTypeStageRegistry.addGenericStage(renderType -> "clouds_target".equals(getOutputName(renderType)), new DynamicBufferShard("clouds", () -> Minecraft.getInstance().levelRenderer.getCloudsTarget()));
        RenderTypeStageRegistry.addGenericStage(renderType -> "item_entity_target".equals(getOutputName(renderType)), new DynamicBufferShard("item_entity", () -> Minecraft.getInstance().levelRenderer.getItemEntityTarget()));
        PostPipelineStageRegistry.bootstrap();
        LightTypeRegistry.bootstrap();
        RenderTypeLayerRegistry.bootstrap();
        VeilResourceEditorRegistry.bootstrap();
        EmitterShapeRegistry.bootstrap();
        RenderStyleRegistry.bootstrap();
        ParticleModuleTypeRegistry.bootstrap();
    }

    private static String getOutputName(RenderType.CompositeRenderType renderType) {
        return ((RenderStateShardAccessor) ((CompositeStateAccessor) (Object) renderType.state()).getOutputState()).getName();
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
