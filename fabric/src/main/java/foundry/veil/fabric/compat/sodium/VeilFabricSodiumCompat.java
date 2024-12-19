package foundry.veil.fabric.compat.sodium;

import foundry.veil.fabric.ext.ShaderChunkRendererExtension;
import foundry.veil.fabric.mixin.compat.sodium.RenderSectionManagerAccessor;
import foundry.veil.fabric.mixin.compat.sodium.SodiumWorldRendererAccessor;
import foundry.veil.impl.compat.SodiumCompat;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class VeilFabricSodiumCompat implements SodiumCompat {

    private static @NotNull StringBuilder getShaderName(ChunkShaderOptions options) {
        StringBuilder name = new StringBuilder("chunk_shader");
        if (options.fog() == ChunkFogMode.SMOOTH) {
            name.append("_fog_smooth");
        }

        TerrainRenderPass pass = options.pass();
        if (pass.isTranslucent()) {
            name.append("_translucent");
        }
        if (pass.supportsFragmentDiscard()) {
            name.append("_cutout");
        }
        return name;
    }

    @Override
    public Object2IntMap<ResourceLocation> getLoadedShaders() {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer != null) {
            RenderSectionManagerAccessor renderSectionManager = (RenderSectionManagerAccessor) ((SodiumWorldRendererAccessor) worldRenderer).getRenderSectionManager();
            if (renderSectionManager != null && renderSectionManager.getChunkRenderer() instanceof ShaderChunkRendererExtension extension) {
                Object2IntMap<ResourceLocation> shaders = new Object2IntArrayMap<>(extension.veil$getPrograms().size());

                for (Map.Entry<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> entry : extension.veil$getPrograms().entrySet()) {
                    StringBuilder name = getShaderName(entry.getKey());
                    shaders.put(ResourceLocation.fromNamespaceAndPath("sodium", name.toString()), entry.getValue().handle());
                }
                return shaders;
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

    @Override
    public void setActiveBuffers(int activeBuffers) {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer != null) {
            RenderSectionManagerAccessor renderSectionManager = (RenderSectionManagerAccessor) ((SodiumWorldRendererAccessor) worldRenderer).getRenderSectionManager();
            if (renderSectionManager != null && renderSectionManager.getChunkRenderer() instanceof ShaderChunkRendererExtension extension) {
                extension.veil$setActiveBuffers(activeBuffers);
            }
        }
    }

    @Override
    public void markChunksDirty() {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer != null) {
            RenderSectionManagerAccessor renderSectionManager = (RenderSectionManagerAccessor) ((SodiumWorldRendererAccessor) worldRenderer).getRenderSectionManager();

            Long2ReferenceMap<RenderSection> map = renderSectionManager.getSectionByPosition();

            for (long longPos : map.keySet()) {
                SectionPos sectionPos = SectionPos.of(longPos);

                ((SodiumWorldRendererAccessor) worldRenderer).getRenderSectionManager().scheduleRebuild(sectionPos.x(), sectionPos.y(), sectionPos.z(), true);
            }
        }
    }
}
