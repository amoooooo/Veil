package foundry.veil.api.network.handler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Context for server-side packet handling.
 *
 * @author Ocelot
 */
public interface ServerPacketContext extends PacketContext {

    /**
     * @return The Minecraft server instance
     */
    default MinecraftServer server() {
        return this.player().getServer();
    }

    @Override
    @NotNull
    ServerPlayer player();
}
