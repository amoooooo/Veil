package foundry.veil.mixin.resource.accessor;

import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(AtlasSet.class)
public interface ResourceAtlasSetAccessor {

    @Accessor
    Map<ResourceLocation, AtlasSet.AtlasEntry> getAtlases();
}
