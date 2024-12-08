package foundry.veil.fabric.ext;

import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;

import java.util.Map;

public interface ShaderChunkRendererExtension {

    void veil$recompile();

    void veil$setActiveBuffers(int activeBuffers);

    Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> veil$getPrograms();
}
