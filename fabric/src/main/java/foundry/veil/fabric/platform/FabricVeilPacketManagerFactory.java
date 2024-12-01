package foundry.veil.fabric.platform;

import foundry.veil.api.network.VeilPacketManager;
import foundry.veil.fabric.network.FabricVeilPacketManager;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class FabricVeilPacketManagerFactory implements VeilPacketManager.Factory {

    @Override
    public VeilPacketManager create(String version) {
        return new FabricVeilPacketManager();
    }
}
