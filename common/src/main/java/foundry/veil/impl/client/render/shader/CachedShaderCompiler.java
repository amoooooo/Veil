package foundry.veil.impl.client.render.shader;

import foundry.veil.api.client.render.shader.CompiledShader;
import foundry.veil.api.client.render.shader.ShaderCompiler;
import foundry.veil.api.client.render.shader.ShaderException;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
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

    private final Map<Integer, CompiledShader> shaders;

    public CachedShaderCompiler(@Nullable ResourceProvider provider) {
        super(provider);
        this.shaders = new HashMap<>();
    }

    private boolean shouldCache(ShaderCompiler.Context context) {
        var def = context.definition();
        if (def == null) return true;
        // Don't use cache if the shader has default preprocessor definitions,
        // in case there are multiple program definitions using the same shader
        return def.definitionDefaults().isEmpty();
    }

    @Override
    public CompiledShader compile(ShaderCompiler.Context context, int type, ProgramDefinition.SourceType sourceType, String source) throws IOException, ShaderException {
        if (!shouldCache(context)) return super.compile(context, type, sourceType, source);

        int hash = Objects.hash(type, source);
        if (this.shaders.containsKey(hash)) {
            return this.shaders.get(hash);
        }
        CompiledShader shader = super.compile(context, type, sourceType, source);
        this.shaders.put(hash, shader);
        return shader;
    }

    @Override
    public void free() {
        super.free();
        this.shaders.clear();
    }
}
