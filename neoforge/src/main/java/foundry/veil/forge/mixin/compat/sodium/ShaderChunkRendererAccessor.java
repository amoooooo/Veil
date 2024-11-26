package foundry.veil.forge.mixin.compat.sodium;

import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ShaderChunkRenderer.class)
public interface ShaderChunkRendererAccessor {

    @Accessor("programs")
    Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> getPrograms();
}
