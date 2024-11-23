package foundry.veil.impl.client.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.*;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import foundry.veil.impl.client.render.pipeline.VeilShaderUploader;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

/**
 * Creates a new shader and compiles each time {@link #compile(Context, int, ProgramDefinition.SourceType, VeilShaderSource)} is called.
 * This should only be used for compiling single shaders.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class DirectShaderCompiler implements ShaderCompiler {

    private final ShaderProvider provider;

    public DirectShaderCompiler(@Nullable ShaderProvider provider) {
        this.provider = provider;
    }

    private void validateType(int type) throws ShaderException {
        if (type == GL_COMPUTE_SHADER && !VeilRenderSystem.computeSupported()) {
            throw new ShaderException("Compute is not supported", null);
        }
    }

    @Override
    public CompiledShader compile(ShaderCompiler.Context context, int type, ProgramDefinition.SourceType sourceType, ResourceLocation path) throws IOException, ShaderException {
        if (this.provider == null) {
            throw new IOException("Failed to read " + ShaderManager.getTypeName(type) + " from " + path + " because no provider was specified");
        }
        VeilShaderSource source = this.provider.getShader(path);
        if (source == null) {
            throw new IOException("Unknown shader: " + path);
        }
        return this.compile(context, type, sourceType, source);
    }

    @Override
    public CompiledShader compile(ShaderCompiler.Context context, int type, ProgramDefinition.SourceType sourceType, VeilShaderSource source) throws ShaderException {
        this.validateType(type);

        String sourceCode = source.sourceCode();
        ResourceLocation sourceId = source.sourceId();
        int shader = glCreateShader(type);
        switch (sourceType) {
            case GLSL -> GlStateManager.glShaderSource(shader, List.of(sourceCode));
            case GLSL_SPIRV -> VeilShaderUploader.get().compile(shader, type, sourceId.toString(), sourceCode, false);
            case HLSL_SPIRV -> VeilShaderUploader.get().compile(shader, type, sourceId.toString(), sourceCode, true);
            case SPIRV -> throw new UnsupportedOperationException("TODO implement");
        }

        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
            String log = glGetShaderInfoLog(shader);
            if (Veil.VERBOSE_SHADER_ERRORS) {
                log += "\n" + sourceCode;
            }
            glDeleteShader(shader); // Delete to prevent leaks
            throw new ShaderException("Failed to compile " + ShaderManager.getTypeName(type) + " shader", log);
        }

        return new CompiledShader(sourceId, shader, Object2IntMaps.unmodifiable(source.uniformBindings()), Collections.unmodifiableSet(source.definitionDependencies()), Collections.unmodifiableSet(source.includes()));
    }

    @Override
    public void free() {
    }
}
