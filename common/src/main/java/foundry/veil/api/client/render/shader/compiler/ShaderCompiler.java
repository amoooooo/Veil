package foundry.veil.api.client.render.shader.compiler;

import foundry.veil.impl.client.render.shader.compiler.CachedShaderCompiler;
import foundry.veil.impl.client.render.shader.compiler.DirectShaderCompiler;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <p>Creates and compiles shaders for shader programs.</p>
 * <p>Create a compiler using {@link #direct(ShaderProvider)} for a single program,
 * or {@link #cached(ShaderProvider)} if compiling multiple.</p>
 *
 * @author Ocelot
 */
public interface ShaderCompiler extends NativeResource {

    /**
     * Creates a new shader and attempts to attach sources read from file to it.
     * The sources are read from
     * The shader will automatically be deleted at some point in the future.
     *
     * @param type The type of shader to create
     * @param path The location of the shader to attach
     * @return A new shader that can be attached to programs
     * @throws IOException     If the file could not be found
     * @throws ShaderException If an error occurs while compiling the shader
     */
    CompiledShader compile(int type, ResourceLocation path) throws IOException, ShaderException;

    /**
     * Creates a new shader and attempts to attach the specified sources to it.
     * The shader will automatically be deleted at some point in the future.
     *
     * @param type   The type of shader to create
     * @param source The source of the shader to attach
     * @return A new shader that can be attached to programs
     * @throws ShaderException If an error occurs while compiling the shader
     */
    CompiledShader compile(int type, VeilShaderSource source) throws ShaderException;

    /**
     * Constructs a shader compiler that creates a new shader for each requested type.
     *
     * @param provider The source of shader files
     * @return shader compiler
     */
    static ShaderCompiler direct(@Nullable ShaderProvider provider) {
        return new DirectShaderCompiler(provider);
    }

    /**
     * Constructs a shader compiler that caches duplicate shader sources.
     *
     * @param provider The source of shader files
     * @return cached shader compiler
     */
    static ShaderCompiler cached(@Nullable ShaderProvider provider) {
        return new CachedShaderCompiler(provider);
    }

    /**
     * Provides shader sources for the compiler.
     */
    @FunctionalInterface
    interface ShaderProvider {

        /**
         * Reads a shader source by location.
         *
         * @param location The location of the shader to retrieve
         * @return The shader source found
         * @throws FileNotFoundException If the shader source does not exist
         */
        VeilShaderSource getShader(ResourceLocation location) throws FileNotFoundException;
    }
}
