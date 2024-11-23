package foundry.veil.api.client.render.shader.processor;

import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Modifies the source code of a shader before compilation.
 *
 * @author Ocelot
 */
public interface ShaderPreProcessor {

    ShaderPreProcessor NOOP = (ctx, source) -> source;
    Pattern UNIFORM_PATTERN = Pattern.compile(".*uniform\\s+(?<type>\\w+)\\W(?<name>\\w*)");

    /**
     * Called once when a shader is first run through the pre-processor.
     */
    default void prepare() {
    }

    /**
     * Modifies the specified shader source input.
     *
     * @param ctx    Context for modifying shaders
     * @param source The GLSL source code to modify
     * @return The modified source or the input if nothing changed
     * @throws IOException If any error occurs while editing the source
     */
    String modify(Context ctx, String source) throws IOException;

    static ShaderPreProcessor allOf(ShaderPreProcessor... processors) {
        return allOf(Arrays.asList(processors));
    }

    static ShaderPreProcessor allOf(Collection<ShaderPreProcessor> processors) {
        List<ShaderPreProcessor> list = new ArrayList<>(processors.size());
        for (ShaderPreProcessor processor : processors) {
            if (processor instanceof ShaderMultiProcessor(ShaderPreProcessor[] values)) {
                list.addAll(Arrays.asList(values));
            } else if (processor != NOOP) {
                list.add(processor);
            }
        }

        if (list.isEmpty()) {
            return NOOP;
        }
        if (list.size() == 1) {
            return list.getFirst();
        }
        return new ShaderMultiProcessor(list.toArray(ShaderPreProcessor[]::new));
    }

    /**
     * Context for modifying source code and shader behavior.
     */
    interface Context {

        /**
         * Runs the specified source through the entire processing list.
         *
         * @param name   The name of the shader file to modify or <code>null</code> if the source is a raw string
         * @param source The shader source code to modify
         * @return The modified source
         * @throws IOException If any error occurs while editing the source
         */
        String modify(@Nullable ResourceLocation name, String source) throws IOException;

        /**
         * Sets the uniform binding for a shader.
         *
         * @param name    The name of the uniform
         * @param binding The binding to set it to
         */
        void addUniformBinding(String name, int binding);

        /**
         * Marks this shader as dependent on the specified pre-definition.
         * When definitions change, only shaders marked as dependent on that definition will be recompiled.
         *
         * @param name The name of the definition to depend on
         */
        void addDefinitionDependency(String name);

        /**
         * @param name The name of the definition to depend on
         */
        void addInclude(ResourceLocation name);

        /**
         * @return A view of all includes in this shader
         */
        Set<ResourceLocation> includes();

        /**
         * @return The id of the shader being compiled or <code>null</code> if the shader is compiled from a raw string
         */
        @Nullable
        ResourceLocation name();

        /**
         * @return Whether the processor is being run for a source file and not a #include file
         */
        boolean isSourceFile();

        /**
         * @return The definition of the program this is being compiled for or <code>null</code> if the shader is standalone
         */
        @Nullable
        ProgramDefinition definition();

        /**
         * @return The set of pre-definitions for shaders
         */
        @Nullable
        ShaderPreDefinitions preDefinitions();

        /**
         * @return The OpenGL type of the compiling shader
         */
        int type();
    }
}
