package foundry.veil.api.client.render.shader.processor;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Adds the version and required extensions for all shaders that do not define a version.
 *
 * @author Ocelot
 */
public class ShaderVersionProcessor implements ShaderPreProcessor {

    public static final Pattern PATTERN = Pattern.compile("#version\\s+.+");

    @Override
    public @NotNull String modify(@NotNull Context context, String source) {
        if (!ShaderVersionProcessor.PATTERN.matcher(source).find()) {
            return "#version 410 core\n" + source;
        }
        return source;
    }
}
