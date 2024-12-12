package foundry.veil.api.network;

import foundry.veil.api.network.handler.ClientPacketContext;
import foundry.veil.api.network.handler.PacketContext;
import foundry.veil.api.network.handler.ServerPacketContext;
import foundry.veil.impl.network.ClientPacketSink;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Manages packet registration and sending.
 *
 * @author Ocelot
 */
public interface VeilPacketManager {

    /**
     * The singleton instance of the {@link VeilPacketManager.Factory}. This is different on each loader.
     */
    VeilPacketManager.Factory FACTORY = ServiceLoader.load(VeilPacketManager.Factory.class).findFirst().orElseThrow(() -> new RuntimeException("Failed to load packet provider"));

    /**
     * Creates a {@link VeilPacketManager}.
     *
     * @param modId   The id of the mod creating the channel
     * @param version The NeoForge channel version
     * @return The packet manager
     */
    static VeilPacketManager create(String modId, String version) {
        return FACTORY.create(modId, version);
    }

    /**
     * Registers a new packet from the client to the server.
     *
     * @param id      The id of the packet
     * @param codec   The codec for encoding and decoding the packet
     * @param handler The handler method for the packet on the client
     * @param <T>     The type of packet to register
     */
    <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PacketHandler<ClientPacketContext, T> handler);

    /**
     * Registers a new packet from the server to the client.
     *
     * @param id      The id of the packet
     * @param codec   The codec for encoding and decoding the packet
     * @param handler The handler method for the packet on the server
     * @param <T>     The type of packet to register
     */
    <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PacketHandler<ServerPacketContext, T> handler);

    /**
     * @return A sink to send packets to the server
     */
    static PacketSink server() {
        return ClientPacketSink.INSTANCE;
    }

    /**
     * @return A sink to send packets to the specified player
     */
    static PacketSink player(ServerPlayer player) {
        return packet -> player.connection.send(packet);
    }

    /**
     * Sends packets to all players in the specified level.
     *
     * @param level The level to send the packet to
     * @return A sink to send packets to all players in the dimension
     */
    static PacketSink level(ServerLevel level) {
        return packet -> level.getServer().getPlayerList().broadcastAll(packet, level.dimension());
    }

    /**
     * Sends packets to all players in the area covered by the specified radius, around the specified coordinates, in the specified dimension, excluding the specified excluded player if present.
     *
     * @param excluded The player to exclude when sending the packet or <code>null</code> to send to all players
     * @param level    the level to send the packet around
     * @param x        the X position
     * @param y        the Y position
     * @param z        the Z position
     * @param radius   the maximum distance from the position in blocks
     * @return A sink to send packets to all players in the area
     */
    static PacketSink around(@Nullable ServerPlayer excluded, ServerLevel level, double x, double y, double z, double radius) {
        return packet -> level.getServer().getPlayerList().broadcast(excluded, x, y, z, radius, level.dimension(), packet);
    }

    /**
     * Sends packets to all players in the server.
     *
     * @param server The server instance
     * @return A sink to send packets to all players
     */
    static PacketSink all(MinecraftServer server) {
        return packet -> server.getPlayerList().broadcastAll(packet);
    }

    /**
     * Sends packets to all players tracking the specified entity, excluding the entity.
     *
     * @param entity The entity to send tracking packets to
     * @return A sink to send packets to all players tracking the entity
     * @throws IllegalArgumentException if the entity is not in a server world
     */
    static PacketSink tracking(Entity entity) {
        return packet -> {
            if (!(entity.level().getChunkSource() instanceof ServerChunkCache chunkCache)) {
                throw new IllegalStateException("Cannot send clientbound payloads on the client");
            }

            chunkCache.broadcast(entity, packet);
        };
    }

    /**
     * Sends packets to all players tracking the specified entity, including the entity.
     *
     * @param entity The entity to send tracking packets to
     * @return A sink to send packets to all players tracking the entity and the entity
     * @throws IllegalArgumentException if the entity is not in a server world
     */
    static PacketSink trackingAndSelf(Entity entity) {
        return packet -> {
            if (!(entity.level().getChunkSource() instanceof ServerChunkCache chunkCache)) {
                throw new IllegalStateException("Cannot send clientbound payloads on the client");
            }

            chunkCache.broadcastAndSend(entity, packet);
        };
    }

    /**
     * Sends packets to all players tracking the chunk at the specified position in the specified level
     *
     * @param level The level to send the packet to
     * @param pos   The chunk to send to
     * @return A sink to send packets to all players tracking that chunk
     */
    static PacketSink tracking(ServerLevel level, ChunkPos pos) {
        return packet -> {
            for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(pos, false)) {
                player.connection.send(packet);
            }
        };
    }

    /**
     * Sends packets to all players tracking the chunk at the specified position in the specified level
     *
     * @param level The level to send the packet to
     * @param pos   The position to send to
     * @return A sink to send packets to all players tracking that chunk
     */
    static PacketSink tracking(ServerLevel level, BlockPos pos) {
        return tracking(level, new ChunkPos(pos));
    }

    /**
     * Sends packets to all players tracking the chunks at the specified positions in the specified level
     *
     * @param level The level to send the packet to
     * @param min   The minimum position to send to
     * @param max   The maximum position to send to
     * @return A sink to send packets to all players tracking that chunk
     */
    static PacketSink tracking(ServerLevel level, BlockPos min, BlockPos max) {
        return packet -> ChunkPos.rangeClosed(new ChunkPos(min), new ChunkPos(max))
                .flatMap(pos -> level.getChunkSource().chunkMap.getPlayers(pos, false).stream())
                .distinct()
                .forEach(player -> player.connection.send(packet));
    }

    /**
     * Sends packets to all players tracking the specified block entity.
     *
     * @param blockEntity The block entity to send the packet to
     * @return A sink to send packets to all players tracking that block entity
     * @throws IllegalArgumentException if the block entity is not in a server level
     */
    static PacketSink tracking(BlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
            throw new IllegalArgumentException("Only supported on server levels!");
        }
        return tracking(serverLevel, blockEntity.getBlockPos());
    }

    /**
     * Handles packets from the client/server.
     *
     * @param <T> The context to use
     * @param <P> The packet to handle
     */
    @FunctionalInterface
    interface PacketHandler<T extends PacketContext, P extends CustomPacketPayload> {

        /**
         * Handles the specified packet.
         *
         * @param payload The packet payload
         * @param ctx     The sided context
         */
        void handlePacket(P payload, T ctx);
    }

    /**
     * Sends packets to players and automatically bundles payloads together.
     */
    @FunctionalInterface
    interface PacketSink {

        /**
         * Sends one or more payloads in a single packet.
         *
         * @param payloads All packets to send
         */
        default void sendPacket(CustomPacketPayload... payloads) {
            if (payloads.length == 0) {
                return;
            }
            if (payloads.length == 1) {
                this.sendPacket(new ClientboundCustomPayloadPacket(payloads[0]));
                return;
            }

            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            for (CustomPacketPayload payload : payloads) {
                packets.add(new ClientboundCustomPayloadPacket(payload));
            }
            this.sendPacket(new ClientboundBundlePacket(packets));
        }

        /**
         * Sends a single packet.
         *
         * @param packet the packet to send
         */
        void sendPacket(Packet<?> packet);
    }

    /**
     * Factory class for {@link VeilPacketManager registration providers}. <br>
     * This class is loaded using {@link ServiceLoader Service Loaders}, and only one
     * should exist per mod loader.
     */
    @ApiStatus.Internal
    interface Factory {

        /**
         * Creates a {@link VeilPacketManager}.
         *
         * @param modId   The id of the mod to register the channel under
         * @param version The NeoForge channel version
         * @return The packet manager
         */
        VeilPacketManager create(String modId, String version);
    }
}
