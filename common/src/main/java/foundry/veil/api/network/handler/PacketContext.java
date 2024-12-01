package foundry.veil.api.network.handler;

import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Common context for packet handling.
 *
 * @author Ocelot
 */
public interface PacketContext {

    /**
     * @return The player that received the payload
     */
    @Nullable
    Player player();

    /**
     * @return The level the context player is on
     */
    default @Nullable Level level() {
        Player player = this.player();
        return player != null ? player.level() : null;
    }

    /**
     * Creates a packet from a packet payload.
     *
     * @param payload the packet payload
     */
    Packet<?> createPacket(CustomPacketPayload payload);

    /**
     * Sends a packet to the other side.
     *
     * @param payload the payload to create a packet from
     */
    default void sendPacket(CustomPacketPayload payload) {
        this.sendPacket(this.createPacket(payload), null);
    }

    /**
     * Sends a packet to the other side.
     *
     * @param payload  the payload to create a packet from
     * @param callback an optional callback to execute after the packet is sent, may be <code>null</code>.
     */
    default void sendPacket(CustomPacketPayload payload, @Nullable PacketSendListener callback) {
        this.sendPacket(this.createPacket(payload), callback);
    }

    /**
     * Sends a packet.
     *
     * @param packet the payload
     */
    default void sendPacket(Packet<?> packet) {
        this.sendPacket(packet, null);
    }

    /**
     * Sends a packet.
     *
     * @param packet   the payload
     * @param callback an optional callback to execute after the packet is sent, may be {@code null}.
     */
    void sendPacket(Packet<?> packet, @Nullable PacketSendListener callback);

    /**
     * Disconnects the player.
     *
     * @param disconnectReason the reason for disconnection
     */
    void disconnect(Component disconnectReason);
}
