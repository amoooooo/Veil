package foundry.veil.forge;

import com.google.common.collect.ImmutableList;
import foundry.veil.Veil;
import foundry.veil.VeilClient;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.forge.event.ForgeVeilRegisterBlockLayersEvent;
import foundry.veil.forge.event.ForgeVeilRegisterFixedBuffersEvent;
import foundry.veil.forge.event.ForgeVeilRendererAvailableEvent;
import foundry.veil.impl.VeilBuiltinPacks;
import foundry.veil.impl.VeilReloadListeners;
import foundry.veil.impl.client.render.VeilUITooltipRenderer;
import foundry.veil.impl.client.render.shader.VeilVanillaShaders;
import foundry.veil.mixin.accessor.RenderStateShardAccessor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Mod(value = Veil.MODID, dist = Dist.CLIENT)
public class VeilForgeClient {

    public VeilForgeClient(IEventBus modEventBus) {
        VeilClient.init();

        modEventBus.addListener(VeilForgeClient::registerKeys);
        modEventBus.addListener(VeilForgeClient::registerGuiOverlays);
        modEventBus.addListener(VeilForgeClient::registerListeners);
        modEventBus.addListener(VeilForgeClient::registerShaders);
        modEventBus.addListener(VeilForgeClient::addPackFinders);

        ImmutableList.Builder<RenderType> blockLayers = ImmutableList.builder();
        ModLoader.postEvent(new ForgeVeilRegisterBlockLayersEvent(renderType -> {
            if (Veil.platform().isDevelopmentEnvironment() && renderType.bufferSize() > RenderType.SMALL_BUFFER_SIZE) {
                Veil.LOGGER.warn("Block render layer '{}' uses a large buffer size: {}. If this is intended you can ignore this message", ((RenderStateShardAccessor) renderType).getName(), renderType.bufferSize());
            }
            blockLayers.add(renderType);
        }));
        ForgeRenderTypeStageHandler.setBlockLayers(blockLayers);

//        if (SodiumCompat.isEnabled()) {
//            SodiumCompat.setLoadedShadersSupplier(() -> {
//                SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
//                if (worldRenderer != null) {
//                    RenderSectionManagerAccessor renderSectionManager = (RenderSectionManagerAccessor) ((SodiumWorldRendererAccessor) worldRenderer).getRenderSectionManager();
//                    if (renderSectionManager != null && renderSectionManager.getChunkRenderer() instanceof ShaderChunkRendererAccessor accessor) {
//                        return Object2IntMaps.singleton(ResourceLocation.fromNamespaceAndPath("sodium", "chunk_shader"), accessor.getPrograms().values().iterator().next().handle());
//                    }
//                }
//                return Object2IntMaps.emptyMap();
//            });
//        }
    }

    private static void registerListeners(RegisterClientReloadListenersEvent event) {
        VeilClient.initRenderer();
        VeilReloadListeners.registerListeners((type, id, listener) -> event.registerReloadListener(listener));
        ModLoader.postEvent(new ForgeVeilRendererAvailableEvent(VeilRenderSystem.renderer()));
        ModLoader.postEvent(new ForgeVeilRegisterFixedBuffersEvent(ForgeRenderTypeStageHandler::register));
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(VeilClient.EDITOR_KEY);
    }

    private static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Veil.veilPath("uitooltip"), VeilUITooltipRenderer::renderOverlay);
    }

    private static void registerShaders(RegisterShadersEvent event) {
        try {
            VeilVanillaShaders.registerShaders((id, vertexFormat, loadCallback) -> event.registerShader(new ShaderInstance(event.getResourceProvider(), id, vertexFormat), loadCallback));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TODO allow pack enabled by default
    private static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            VeilBuiltinPacks.registerPacks((id, defaultEnabled) -> event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "resourcepacks/" + id.getPath()),
                    PackType.CLIENT_RESOURCES,
                    Component.literal(id.getNamespace() + "/" + id.getPath()),
                    PackSource.DEFAULT,
                    false,
                    Pack.Position.TOP
            ));
        }
    }
}
