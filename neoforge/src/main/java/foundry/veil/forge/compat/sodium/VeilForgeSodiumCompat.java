package foundry.veil.forge.compat.sodium;

import foundry.veil.forge.ext.ShaderChunkRendererExtension;
import foundry.veil.forge.mixin.compat.sodium.RenderSectionManagerAccessor;
import foundry.veil.forge.mixin.compat.sodium.SodiumWorldRendererAccessor;
import foundry.veil.impl.compat.SodiumCompat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.resources.ResourceLocation;

public class VeilForgeSodiumCompat implements SodiumCompat {

    @Override
    public Object2IntMap<ResourceLocation> getLoadedShaders() {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer != null) {
            RenderSectionManagerAccessor renderSectionManager = (RenderSectionManagerAccessor) ((SodiumWorldRendererAccessor) worldRenderer).getRenderSectionManager();
            if (renderSectionManager != null && renderSectionManager.getChunkRenderer() instanceof ShaderChunkRendererExtension extension) {
                return Object2IntMaps.singleton(ResourceLocation.fromNamespaceAndPath("sodium", "chunk_shader"), extension.veil$getPrograms().values().iterator().next().handle());
            }
        }
        return Object2IntMaps.emptyMap();
    }

    @Override
    public void recompile() {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer != null) {
            RenderSectionManagerAccessor renderSectionManager = (RenderSectionManagerAccessor) ((SodiumWorldRendererAccessor) worldRenderer).getRenderSectionManager();
            if (renderSectionManager != null && renderSectionManager.getChunkRenderer() instanceof ShaderChunkRendererExtension extension) {
                extension.veil$recompile();
            }
        }
    }
}
