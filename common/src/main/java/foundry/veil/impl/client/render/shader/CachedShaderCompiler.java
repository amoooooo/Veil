package foundry.veil.impl.client.render.shader;

import foundry.veil.api.client.render.shader.CompiledShader;
import foundry.veil.api.client.render.shader.ShaderCompiler;
import foundry.veil.api.client.render.shader.ShaderException;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Attempts to cache the exact same shader sources to reduce the number of compiled shaders.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class CachedShaderCompiler extends DirectShaderCompiler {

    private final Int2ObjectMap<CompiledShader> shaders;

    public CachedShaderCompiler(@Nullable ResourceProvider provider) {
        super(provider);
        this.shaders = new Int2ObjectArrayMap<>();
    }

    @Override
    public CompiledShader compile(ShaderCompiler.Context context, int type, ProgramDefinition.SourceType sourceType, ResourceLocation id, int flags) throws IOException, ShaderException {
        int hash = Objects.hash(type, id);
        if (this.shaders.containsKey(hash)) {
            return this.shaders.get(hash);
        }
        CompiledShader shader = super.compile(context, type, sourceType, id, flags);
        this.shaders.put(hash, shader);
        return shader;
    }

    @Override
    public CompiledShader compile(ShaderCompiler.Context context, int type, ProgramDefinition.SourceType sourceType, String source, int flags) throws IOException, ShaderException {
        int hash = Objects.hash(type, source);
        if (this.shaders.containsKey(hash)) {
            return this.shaders.get(hash);
        }
        CompiledShader shader = super.compile(context, type, sourceType, source, flags);
        this.shaders.put(hash, shader);
        return shader;
    }

    @Override
    public void free() {
        super.free();
        this.shaders.clear();
    }
}
