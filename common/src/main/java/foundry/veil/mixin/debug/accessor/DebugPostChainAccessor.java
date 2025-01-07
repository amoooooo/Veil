package foundry.veil.mixin.debug.accessor;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PostChain.class)
public interface DebugPostChainAccessor {

    @Accessor
    List<PostPass> getPasses();
}
