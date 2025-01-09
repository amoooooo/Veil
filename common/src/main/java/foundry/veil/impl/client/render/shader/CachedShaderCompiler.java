package foundry.veil.impl.client.render.shader;

import foundry.veil.api.client.render.shader.CompiledShader;
import foundry.veil.api.client.render.shader.ShaderException;
import foundry.veil.api.client.render.shader.VeilShaderSource;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * Attempts to cache the exact same shader sources to reduce the number of compiled shaders.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class CachedShaderCompiler extends DirectShaderCompiler {

    private final Int2ObjectMap<CompiledShader> shaders;

    public CachedShaderCompiler(@Nullable ShaderProvider provider) {
        super(provider);
        this.shaders = new Int2ObjectArrayMap<>();
    }

    @Override
    public CompiledShader compile(int type, ResourceLocation path) throws IOException, ShaderException {
        int hash = Objects.hash(type, path);
        if (this.shaders.containsKey(hash)) {
            return this.shaders.get(hash);
        }
        CompiledShader shader = super.compile(type, path);
        this.shaders.put(hash, shader);
        return shader;
    }

    @Override
    public CompiledShader compile(int type, VeilShaderSource source) throws ShaderException {
        int hash = Objects.hash(type, source.sourceId());
        if (this.shaders.containsKey(hash)) {
            return this.shaders.get(hash);
        }
        CompiledShader shader = super.compile(type, source);
        this.shaders.put(hash, shader);
        return shader;
    }

    @Override
    public void free() {
        super.free();
        this.shaders.clear();
    }
}
