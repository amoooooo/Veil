package foundry.veil.api.client.render.shader.processor;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds support for <code>layout(binding = #)</code> in the shader source without needing shader version 420.
 *
 * @author Ocelot
 */
public class ShaderBindingProcessor extends ShaderLineProcessor {

    private static final Pattern LAYOUT_PATTERN = Pattern.compile("(?<prefix>.*)layout[(](?<layoutPrefix>.*)binding\\s*=\\s*(?<binding>\\d+)(?<layoutSuffix>.*)[)](?<suffix>.*)");
    private static final Pattern VERSION_PATTERN = Pattern.compile("#version\\s+(\\d+)");

    @Override
    public @NotNull String modify(@NotNull Context context, String source) throws IOException {
        Matcher versionMatcher = VERSION_PATTERN.matcher(source);
        if (versionMatcher.find()) {
            try {
                int version = Integer.parseInt(versionMatcher.group(1));
                if (version >= 420) {
                    return source;
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        return super.modify(context, source);
    }

    @Override
    public void modify(@NotNull Context context, @NotNull String original, @NotNull List<String> line) throws IOException {
        if (!(context instanceof VeilContext veilContext)) {
            return;
        }

        Matcher matcher = LAYOUT_PATTERN.matcher(original);
        if (!matcher.find()) {
            return;
        }

        line.clear();

        String binding = this.group(matcher, "binding");
        if (binding.isEmpty()) {
            throw new IOException("Failed to find 'binding' group: " + original);
        }

        try {
            Matcher uniformMatcher = UNIFORM_PATTERN.matcher(original);
            if (!uniformMatcher.find()) {
                line.add(original);
                return;
            }

            // atomic counters require the binding declaration in the shader
            String type = uniformMatcher.group("type");
            if ("atomic_uint".equals(type)) {
                line.add(original);
                return;
            }

            String name = uniformMatcher.group("name");
            if (name == null && type == null) {
                line.add(original);
                return;
            }
            veilContext.addUniformBinding(name != null && !name.isEmpty() ? name : type, Integer.parseInt(binding));
        } catch (Exception e) {
            throw new IOException(e);
        }

        String prefix = this.group(matcher, "prefix");
        String layoutPrefix = this.group(matcher, "layoutPrefix");
        String layoutSuffix = this.group(matcher, "layoutSuffix");
        String suffix = this.group(matcher, "suffix");

        if (layoutPrefix.isEmpty() && layoutSuffix.isEmpty()) {
            line.add(prefix + suffix);
            return;
        }

        if (layoutSuffix.isEmpty() && layoutPrefix.endsWith(",")) {
            layoutPrefix = layoutPrefix.substring(0, layoutPrefix.length() - 1).trim();
        }
        if (layoutSuffix.startsWith(",")) {
            layoutSuffix = layoutSuffix.substring(1).trim();
        }

        line.add(prefix + "layout(" + layoutPrefix + layoutSuffix + ") " + suffix);
    }

    private @NotNull String group(@NotNull Matcher matcher, @NotNull String name) {
        String group = matcher.group(name);
        return group != null ? group.trim() : "";
    }
}
