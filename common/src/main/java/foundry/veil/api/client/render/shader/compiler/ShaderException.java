package foundry.veil.api.client.render.shader.compiler;

import org.jetbrains.annotations.Nullable;

/**
 * An exception used to indicate shader compilation and linking issues.
 *
 * @author Ocelot
 */
public class ShaderException extends Exception {

    private final String glError;

    public ShaderException(String error) {
        this(error, null);
    }

    public ShaderException(String error, @Nullable String glError) {
        super(error);
        this.glError = glError;
    }

    /**
     * @return The OpenGL shader error.
     */
    public @Nullable String getGlError() {
        return this.glError;
    }
}
