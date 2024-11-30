package foundry.veil.forge.ext;

import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;

import java.util.Map;

public interface ShaderChunkRendererExtension {

    void veil$recompile();

    Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> veil$getPrograms();
}
