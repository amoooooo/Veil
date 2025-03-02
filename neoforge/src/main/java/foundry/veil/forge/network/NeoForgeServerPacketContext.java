package foundry.veil.forge.network;

import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public record NeoForgeServerPacketContext(IPayloadContext context) implements ServerPacketContext {

    @Override
    public @NotNull ServerPlayer player() {
        return (ServerPlayer) this.context.player();
    }

    @Override
    public Packet<?> createPacket(CustomPacketPayload payload) {
        return new ClientboundCustomPayloadPacket(payload);
    }

    @Override
    public void sendPacket(Packet<?> packet, @Nullable PacketSendListener callback) {
        this.context.connection().send(packet, callback);
    }

    @Override
    public void disconnect(Component disconnectReason) {
        this.context.disconnect(disconnectReason);
    }
}
