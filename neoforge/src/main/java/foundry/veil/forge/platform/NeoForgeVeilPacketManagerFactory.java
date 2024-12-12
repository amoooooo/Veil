package foundry.veil.forge.platform;

import foundry.veil.api.network.VeilPacketManager;
import foundry.veil.forge.network.NeoForgeVeilPacketManager;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class NeoForgeVeilPacketManagerFactory implements VeilPacketManager.Factory {

    @Override
    public VeilPacketManager create(String modId, String version) {
        return new NeoForgeVeilPacketManager(modId, version);
    }
}
