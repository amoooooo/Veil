package foundry.veil.forge;

import foundry.veil.Veil;
import foundry.veil.forge.platform.NeoForgeVeilEventPlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Mod(Veil.MODID)
public class VeilForge {

    public VeilForge(IEventBus bus) {
        Veil.init();
    }
}
