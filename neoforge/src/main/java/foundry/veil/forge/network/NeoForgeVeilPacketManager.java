package foundry.veil.forge.network;

import foundry.veil.api.network.VeilPacketManager;
import foundry.veil.api.network.handler.ClientPacketContext;
import foundry.veil.api.network.handler.PacketContext;
import foundry.veil.api.network.handler.ServerPacketContext;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

@ApiStatus.Internal
public class NeoForgeVeilPacketManager implements VeilPacketManager {

    private final String version;
    private final Set<RegisteredPacket<ClientPacketContext>> clientPackets;
    private final Set<RegisteredPacket<ServerPacketContext>> serverPackets;

    public NeoForgeVeilPacketManager(String version) {
        this.version = version;
        this.clientPackets = new ObjectArraySet<>();
        this.serverPackets = new ObjectArraySet<>();
    }

    public void register(IEventBus modBus) {
        modBus.addListener(this::registerHandler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerHandler(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(this.version);

        for (RegisteredPacket<ClientPacketContext> packet : this.clientPackets) {
            PacketHandler handler = packet.handler;
            registrar.playToClient((CustomPacketPayload.Type<CustomPacketPayload>) packet.id, (StreamCodec<? super RegistryFriendlyByteBuf, CustomPacketPayload>) packet.codec, (payload, context) -> {
                handler.handlePacket(payload, new NeoForgeClientPacketContext(context));
            });
        }

        for (RegisteredPacket<ServerPacketContext> packet : this.serverPackets) {
            PacketHandler handler = packet.handler;
            registrar.playToServer((CustomPacketPayload.Type<CustomPacketPayload>) packet.id, (StreamCodec<? super RegistryFriendlyByteBuf, CustomPacketPayload>) packet.codec, (payload, context) -> {
                handler.handlePacket(payload, new NeoForgeServerPacketContext(context));
            });
        }
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PacketHandler<ClientPacketContext, T> handler) {
        this.clientPackets.add(new RegisteredPacket<>(id, codec, handler));
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PacketHandler<ServerPacketContext, T> handler) {
        this.serverPackets.add(new RegisteredPacket<>(id, codec, handler));
    }

    private record RegisteredPacket<T extends PacketContext>(
            CustomPacketPayload.Type<? extends CustomPacketPayload> id,
            StreamCodec<? super RegistryFriendlyByteBuf, ?> codec,
            PacketHandler<T, ?> handler) {
    }
}
