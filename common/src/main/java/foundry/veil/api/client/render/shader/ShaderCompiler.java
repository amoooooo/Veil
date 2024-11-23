package foundry.veil.api.client.render.shader;

import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import foundry.veil.impl.client.render.shader.CachedShaderCompiler;
import foundry.veil.impl.client.render.shader.DirectShaderCompiler;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

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
     * @param context The context for compiling the shader
     * @param type    The type of shader to create
     * @param path    The location of the shader to attach
     * @return A new shader that can be attached to programs
     * @throws IOException     If the file could not be found
     * @throws ShaderException If an error occurs while compiling the shader
     */
    CompiledShader compile(Context context, int type, ProgramDefinition.SourceType sourceType, ResourceLocation path) throws IOException, ShaderException;

    /**
     * Creates a new shader and attempts to attach the specified sources to it.
     * The shader will automatically be deleted at some point in the future.
     *
     * @param context The context for compiling the shader
     * @param type    The type of shader to create
     * @param source  The source of the shader to attach
     * @return A new shader that can be attached to programs
     * @throws ShaderException If an error occurs while compiling the shader
     */
    CompiledShader compile(Context context, int type, ProgramDefinition.SourceType sourceType, VeilShaderSource source) throws ShaderException;

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
     * Context for compiling shaders and programs.
     *
     * @param activeBuffers The currently active buffers
     * @param sourceSet     The shader source set to compile for
     * @param definition    The definition the shader is being compiled for
     */
    record Context(int activeBuffers, ShaderSourceSet sourceSet, ProgramDefinition definition) {
    }

    @FunctionalInterface
    interface ShaderProvider {

        @Nullable VeilShaderSource getShader(ResourceLocation name);
    }
}
