package foundry.veil.fabric.network;

import foundry.veil.api.network.handler.ServerPacketContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public record FabricServerPacketContext(ServerPlayNetworking.Context context) implements ServerPacketContext {

    @Override
    public ServerPlayer player() {
        return this.context.player();
    }

    @Override
    public Packet<?> createPacket(CustomPacketPayload payload) {
        return this.context.responseSender().createPacket(payload);
    }

    @Override
    public void sendPacket(Packet<?> packet, @Nullable PacketSendListener callback) {
        this.context.responseSender().sendPacket(packet, callback);
    }

    @Override
    public void disconnect(Component disconnectReason) {
        this.context.responseSender().disconnect(disconnectReason);
    }
}
