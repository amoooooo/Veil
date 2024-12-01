package foundry.veil.api.network.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Context for client-side packet handling.
 *
 * @author Ocelot
 */
public interface ClientPacketContext extends PacketContext {

    /**
     * @return The Minecraft client instance
     */
    Minecraft client();

    @Override
    @Nullable
    LocalPlayer player();
}
