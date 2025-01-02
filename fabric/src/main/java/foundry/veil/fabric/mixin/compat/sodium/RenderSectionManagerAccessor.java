package foundry.veil.fabric.mixin.compat.sodium;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RenderSectionManager.class, remap = false)
public interface RenderSectionManagerAccessor {

    @Accessor
    ChunkRenderer getChunkRenderer();

    @Accessor
    Long2ReferenceMap<RenderSection> getSectionByPosition();

}
