package foundry.veil.forge;

import com.google.common.collect.ImmutableList;
import foundry.veil.Veil;
import foundry.veil.VeilClient;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.forge.event.ForgeVeilRegisterBlockLayerEvent;
import foundry.veil.forge.event.ForgeVeilRegisterFixedBuffersEvent;
import foundry.veil.forge.event.ForgeVeilRendererEvent;
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
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.PathPackResources;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

@ApiStatus.Internal
public class VeilForgeClient {

    public static void init() {
        VeilClient.init();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(VeilForgeClient::registerKeys);
        modEventBus.addListener(VeilForgeClient::registerGuiOverlays);
        modEventBus.addListener(VeilForgeClient::registerListeners);
        modEventBus.addListener(VeilForgeClient::registerShaders);
        modEventBus.addListener(VeilForgeClient::addPackFinders);

        ImmutableList.Builder<RenderType> blockLayers = ImmutableList.builder();
        ModLoader.get().postEvent(new ForgeVeilRegisterBlockLayerEvent(renderType -> {
            if (Veil.platform().isDevelopmentEnvironment() && renderType.bufferSize() > RenderType.SMALL_BUFFER_SIZE) {
                Veil.LOGGER.warn("Block render layer '{}' uses a large buffer size: {}. If this is intended you can ignore this message", ((RenderStateShardAccessor) renderType).getName(), renderType.bufferSize());
            }
            blockLayers.add(renderType);
        }));
        ForgeRenderTypeStageHandler.setBlockLayers(blockLayers);
    }

    private static void registerListeners(RegisterClientReloadListenersEvent event) {
        VeilClient.initRenderer();
        VeilReloadListeners.registerListeners((type, id, listener) -> event.registerReloadListener(listener));
        ModLoader loader = ModLoader.get();
        loader.postEvent(new ForgeVeilRendererEvent(VeilRenderSystem.renderer()));
        loader.postEvent(new ForgeVeilRegisterFixedBuffersEvent(ForgeRenderTypeStageHandler::register));
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(VeilClient.EDITOR_KEY);
    }

    private static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "uitooltip", VeilUITooltipRenderer::renderOverlay);
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
            VeilBuiltinPacks.registerPacks((id, defaultEnabled) -> registerBuiltinPack(event, id));
        }
    }

    private static void registerBuiltinPack(AddPackFindersEvent event, ResourceLocation id) {
        Path resourcePath = ModList.get().getModFileById(Veil.MODID).getFile().findResource("resourcepacks/" + id.getPath());
        Pack pack = Pack.readMetaAndCreate(id.toString(), Component.literal(id.getNamespace() + "/" + id.getPath()), false, packId -> new PathPackResources(packId, true, resourcePath), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack == null) {
            Veil.LOGGER.error("Failed to find builtin pack: {}", id);
            return;
        }
        event.addRepositorySource(packConsumer -> packConsumer.accept(pack));
    }
}
