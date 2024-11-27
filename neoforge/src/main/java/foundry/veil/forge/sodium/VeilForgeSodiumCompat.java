package foundry.veil.forge.sodium;

import foundry.veil.forge.mixin.compat.sodium.RenderSectionManagerAccessor;
import foundry.veil.forge.mixin.compat.sodium.ShaderChunkRendererAccessor;
import foundry.veil.forge.mixin.compat.sodium.SodiumWorldRendererAccessor;
import foundry.veil.impl.compat.SodiumCompat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class VeilForgeSodiumCompat implements SodiumCompat {

    @Override
    public Object2IntMap<ResourceLocation> getLoadedShaders() {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer != null) {
            RenderSectionManagerAccessor renderSectionManager = (RenderSectionManagerAccessor) ((SodiumWorldRendererAccessor) worldRenderer).getRenderSectionManager();
            if (renderSectionManager != null && renderSectionManager.getChunkRenderer() instanceof ShaderChunkRendererAccessor accessor) {
                return Object2IntMaps.singleton(ResourceLocation.fromNamespaceAndPath("sodium", "chunk_shader"), accessor.getPrograms().values().iterator().next().handle());
            }
        }
        return Object2IntMaps.emptyMap();
    }

    @Override
    public void recompile() {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer != null) {
            RenderSectionManagerAccessor renderSectionManager = (RenderSectionManagerAccessor) ((SodiumWorldRendererAccessor) worldRenderer).getRenderSectionManager();
            if (renderSectionManager != null && renderSectionManager.getChunkRenderer() instanceof ShaderChunkRendererAccessor accessor) {
                Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> programs = accessor.getPrograms();
                for (GlProgram<ChunkShaderInterface> program : programs.values()) {
                    program.delete();
                }
                programs.clear();
            }
        }
    }
}
