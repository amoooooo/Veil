package foundry.veil.mixin.pipeline.accessor;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ReloadableResourceManager.class)
public interface PipelineReloadableResourceManagerAccessor {

    @Accessor
    List<PreparableReloadListener> getListeners();
}
