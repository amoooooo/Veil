package foundry.veil.forge;

import foundry.veil.Veil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Mod(Veil.MODID)
public class VeilForge {

    public VeilForge() {
        Veil.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> VeilForgeClient::init);
    }
}
