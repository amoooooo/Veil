package foundry.veil.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import foundry.veil.Veil;
import foundry.veil.VeilClient;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.quasar.data.QuasarParticles;
import foundry.veil.api.quasar.particle.ParticleEmitter;
import foundry.veil.api.quasar.particle.ParticleSystemManager;
import foundry.veil.fabric.util.FabricReloadListener;
import foundry.veil.impl.ClientEnumArgument;
import foundry.veil.impl.VeilBuiltinPacks;
import foundry.veil.impl.VeilReloadListeners;
import foundry.veil.impl.client.render.VeilUITooltipRenderer;
import foundry.veil.impl.client.render.shader.VeilVanillaShaders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;

@ApiStatus.Internal
public class VeilFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        VeilClient.init();
        HudRenderCallback.EVENT.register(VeilUITooltipRenderer::renderOverlay);
        ClientTickEvents.END_CLIENT_TICK.register(client -> VeilClient.tickClient(client.getTimer().getRealtimeDeltaTicks()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(VeilRenderSystem.renderer().getLightRenderer()::free));

        KeyBindingHelper.registerKeyBinding(VeilClient.EDITOR_KEY);

        // Register test resource pack
        ModContainer container = FabricLoader.getInstance().getModContainer(Veil.MODID).orElseThrow();
        VeilBuiltinPacks.registerPacks((id, defaultEnabled) -> ResourceManagerHelper.registerBuiltinResourcePack(id, container, defaultEnabled ? ResourcePackActivationType.DEFAULT_ENABLED : ResourcePackActivationType.NORMAL));

        CoreShaderRegistrationCallback.EVENT.register(context -> VeilVanillaShaders.registerShaders(context::register));
        VeilReloadListeners.registerListeners((type, id, listener) -> ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new FabricReloadListener(Veil.veilPath(id), listener)));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = LiteralArgumentBuilder.literal("quasar");
            builder.then(ClientCommandManager.argument("emitter", ResourceLocationArgument.id()).suggests(QuasarParticles.emitterSuggestionProvider()).then(ClientCommandManager.argument("position", Vec3Argument.vec3()).executes(ctx -> {
                ResourceLocation id = ctx.getArgument("emitter", ResourceLocation.class);

                FabricClientCommandSource source = ctx.getSource();
                ParticleSystemManager particleManager = VeilRenderSystem.renderer().getParticleManager();
                ParticleEmitter instance = particleManager.createEmitter(id);
                if (instance == null) {
                    source.sendError(Component.literal("Unknown emitter: " + id));
                    return 0;
                }

                WorldCoordinates coordinates = ctx.getArgument("position", WorldCoordinates.class);
                Vec3 pos = coordinates.getPosition(source.getEntity().createCommandSourceStack());
                instance.setPosition(pos.x, pos.y, pos.z);
                particleManager.addParticleSystem(instance);
                source.sendFeedback(Component.literal("Spawned " + id));
                return 1;
            })));
            dispatcher.register(builder);

            if (Veil.platform().isDevelopmentEnvironment()) {
                ResourceLocation bufferId = Veil.veilPath("forced");
                LiteralArgumentBuilder<FabricClientCommandSource> debugBuilder = LiteralArgumentBuilder.literal("veil");
                debugBuilder.then(ClientCommandManager.literal("buffers")
                        .then(ClientCommandManager.literal("enable")
                                .then(ClientCommandManager.argument("buffer", ClientEnumArgument.enumArgument(DynamicBufferType.class)).executes(ctx -> {
                                    DynamicBufferType value = ctx.getArgument("buffer", DynamicBufferType.class);
                                    VeilRenderSystem.renderer().enableBuffers(bufferId, value);
                                    ctx.getSource().sendFeedback(Component.translatable("commands.veil.buffers.enable", value.name().toLowerCase(Locale.ROOT)));
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(ClientCommandManager.literal("all").executes(ctx -> {
                                    DynamicBufferType[] values = DynamicBufferType.values();
                                    VeilRenderSystem.renderer().enableBuffers(bufferId, values);
                                    ctx.getSource().sendFeedback(Component.translatable("commands.veil.buffers.enable.all"));
                                    return values.length;
                                }))
                        )
                        .then(ClientCommandManager.literal("disable")
                                .then(ClientCommandManager.argument("buffer", ClientEnumArgument.enumArgument(DynamicBufferType.class)).executes(ctx -> {
                                    DynamicBufferType value = ctx.getArgument("buffer", DynamicBufferType.class);
                                    VeilRenderSystem.renderer().disableBuffers(bufferId, value);
                                    ctx.getSource().sendFeedback(Component.translatable("commands.veil.buffers.disable", value.name().toLowerCase(Locale.ROOT)));
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(ClientCommandManager.literal("all").executes(ctx -> {
                                    DynamicBufferType[] values = DynamicBufferType.values();
                                    VeilRenderSystem.renderer().disableBuffers(bufferId, values);
                                    ctx.getSource().sendFeedback(Component.translatable("commands.veil.buffers.disable.all"));
                                    return values.length;
                                }))
                        ));
                dispatcher.register(debugBuilder);
            }
        });
        ClientTickEvents.START_WORLD_TICK.register(client -> VeilRenderSystem.renderer().getParticleManager().tick());
    }
}
