package foundry.veil.mixin.resource.accessor;

import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelManager.class)
public interface ResourceModelManagerAccessor {

    @Accessor
    int getMaxMipmapLevels();

    @Accessor
    AtlasSet getAtlases();
}
