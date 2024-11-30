package foundry.veil.fabric.compat.sodium;

import foundry.veil.fabric.mixin.compat.sodium.RenderSectionManagerAccessor;
import foundry.veil.fabric.ext.ShaderChunkRendererExtension;
import foundry.veil.fabric.mixin.compat.sodium.SodiumWorldRendererAccessor;
import foundry.veil.impl.compat.SodiumCompat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class VeilFabricSodiumCompat implements SodiumCompat {

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
