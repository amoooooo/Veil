package foundry.veil.forge.network;

import foundry.veil.api.network.handler.ClientPacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public record NeoForgeClientPacketContext(IPayloadContext context) implements ClientPacketContext {

    @Override
    public Minecraft client() {
        return Minecraft.getInstance();
    }

    @Override
    public LocalPlayer player() {
        return (LocalPlayer) this.context.player();
    }

    @Override
    public Packet<?> createPacket(CustomPacketPayload payload) {
        return new ServerboundCustomPayloadPacket(payload);
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
