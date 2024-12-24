package foundry.veil.api.client.render.shader.processor;

import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.ShaderImporter;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import foundry.veil.api.glsl.GlslSyntaxException;
import foundry.veil.api.glsl.node.GlslNode;
import foundry.veil.api.glsl.node.GlslRootNode;
import foundry.veil.api.glsl.node.GlslTree;
import foundry.veil.api.glsl.node.function.GlslFunctionNode;
import foundry.veil.api.glsl.node.variable.GlslNewNode;
import foundry.veil.lib.anarres.cpp.LexerException;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_EVALUATION_SHADER;

/**
 * Modifies the source code of a shader before compilation.
 *
 * @author Ocelot
 */
public interface ShaderPreProcessor {

    ShaderPreProcessor NOOP = (ctx, source) -> {
    };

    /**
     * Called once when a shader is first run through the pre-processor.
     */
    default void prepare() {
    }

    /**
     * Modifies the specified shader source input.
     *
     * @param ctx  Context for modifying shaders
     * @param tree The GLSL source code tree to modify
     * @throws IOException         If any error occurs while editing the source
     * @throws GlslSyntaxException If there was an error in the syntax of the source code
     * @throws LexerException      If an error occurs during shader C preprocessing
     */
    void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException;

    /**
     * Creates a composite pre-processor with the specified values.
     *
     * @param processors The processors to run in order
     * @return A new processor that runs all provided processors
     */
    static ShaderPreProcessor allOf(ShaderPreProcessor... processors) {
        return allOf(Arrays.asList(processors));
    }

    /**
     * Creates a composite pre-processor with the specified values.
     *
     * @param processors The processors to run in order
     * @return A new processor that runs all provided processors
     */
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
    sealed interface Context permits MinecraftContext, VeilContext, SodiumContext {

        /**
         * Runs the specified source through the entire processing list.
         *
         * @param name   The name of the shader file to modify or <code>null</code> if the source is a raw string
         * @param source The shader source code to modify
         * @return The modified source
         * @throws IOException         If any error occurs while editing the source
         * @throws GlslSyntaxException If there was an error in the syntax of the source code
         * @throws LexerException      If an error occurs during shader C preprocessing
         */
        GlslTree modifyInclude(@Nullable ResourceLocation name, String source) throws IOException, GlslSyntaxException, LexerException;

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
         * @return The currently active dynamic buffers
         */
        int activeBuffers();

        /**
         * @return The OpenGL type of the compiling shader
         */
        int type();

        /**
         * @return Whether the current shader file is the vertex program
         */
        default boolean isVertex() {
            return this.type() == GL_VERTEX_SHADER;
        }

        /**
         * @return Whether the current shader file is the fragment program
         */
        default boolean isFragment() {
            return this.type() == GL_FRAGMENT_SHADER;
        }

        /**
         * @return Whether the current shader file is the geometry program
         */
        default boolean isGeometry() {
            return this.type() == GL_GEOMETRY_SHADER;
        }

        /**
         * @return Whether the current shader file is the tessellation control program
         */
        default boolean isTessellationControl() {
            return this.type() == GL_TESS_CONTROL_SHADER;
        }

        /**
         * @return Whether the current shader file is the tessellation evaluation program
         */
        default boolean isTessellationEvaluation() {
            return this.type() == GL_TESS_EVALUATION_SHADER;
        }

        /**
         * Loads the specified import from file <code>assets/modid/pinwheel/shaders/include/path.glsl</code> and adds it to this source tree.
         *
         * @param name             The name of the import to load
         * @param tree             The tree to include the file into
         * @param strategy How duplicate shader methods should be handled
         * @throws IOException If there was an error loading the import file
         */
        default void include(GlslTree tree, ResourceLocation name, IncludeOverloadStrategy strategy) throws IOException, GlslSyntaxException, LexerException {
            GlslTree loadedImport = this.shaderImporter().loadImport(this, name, false);
            tree.getDirectives().addAll(loadedImport.getDirectives());

            Set<String> fieldNames = loadedImport.fields()
                    .map(GlslNewNode::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
            Set<String> functionNames = loadedImport.functions()
                    .filter(function -> function.getBody() != null)
                    .map(GlslFunctionNode::getName)
                    .collect(Collectors.toUnmodifiableSet());

            switch (strategy) {
                case FAIL -> {
                    for (GlslNode sourceNode : tree.getBody()) {
                        if (!(sourceNode instanceof GlslRootNode rootNode)) {
                            continue;
                        }

                        if (rootNode.isField()) {
                            GlslNewNode field = rootNode.asField();
                            String fieldName = field.getName();
                            if (fieldName != null && fieldNames.contains(fieldName)) {
                                throw new IOException("Field is part of include '" + name + "': " + fieldName);
                            }
                        } else if (rootNode.isFunction()) {
                            GlslFunctionNode function = rootNode.asFunction();
                            String functionName = function.getName();
                            if (functionNames.contains(functionName)) {
                                throw new IOException("Function is part of include '" + name + "': " + functionName);
                            }
                        }
                    }
                }
                case SOURCE -> {
                    for (GlslNode sourceNode : tree.getBody()) {
                        if (!(sourceNode instanceof GlslRootNode rootNode)) {
                            continue;
                        }

                        if (rootNode.isField()) {
                            GlslNewNode field = rootNode.asField();
                            String fieldName = field.getName();
                            if (fieldName != null && fieldNames.contains(fieldName)) {
                                Set<GlslNewNode> remove = loadedImport.fields()
                                        .filter(node -> fieldName.equals(node.getName()))
                                        .collect(Collectors.toUnmodifiableSet());

                                // Remove fields from include source
                                loadedImport.getBody().removeAll(remove);
                            }
                        } else if (rootNode.isFunction()) {
                            GlslFunctionNode function = rootNode.asFunction();
                            String functionName = function.getName();
                            if (functionNames.contains(functionName)) {
                                Set<GlslFunctionNode> remove = loadedImport.functions()
                                        .filter(node -> node.getName().equals(functionName) && node.getBody() != null && node.getHeader().equals(function.getHeader()))
                                        .collect(Collectors.toUnmodifiableSet());
                                // Remove functions from include source
                                loadedImport.getBody().removeAll(remove);
                            }
                        }
                    }
                }
                case INCLUDE -> {
                    Iterator<GlslNode> iterator = tree.getBody().iterator();
                    while (iterator.hasNext()) {
                        GlslNode sourceNode = iterator.next();
                        if (!(sourceNode instanceof GlslRootNode rootNode)) {
                            continue;
                        }

                        if (rootNode.isField()) {
                            GlslNewNode field = rootNode.asField();
                            if (fieldNames.contains(field.getName())) {
                                // Remove field from shader source
                                iterator.remove();
                            }
                        } else if (rootNode.isFunction()) {
                            GlslFunctionNode function = rootNode.asFunction();
                            String functionName = function.getName();
                            if (functionNames.contains(functionName)) {
                                if (loadedImport.functions().anyMatch(node -> node.getName().equals(functionName) &&
                                        node.getBody() != null &&
                                        node.getHeader().equals(function.getHeader()))) {
                                    // If a function exists in the include that would conflict, remove from the source
                                    iterator.remove();
                                }
                            }
                        }
                    }
                }
            }
            tree.getBody().addAll(0, loadedImport.getBody());
        }

        /**
         * @return The importer instance
         * @see #include(GlslTree, ResourceLocation, IncludeOverloadStrategy)
         */
        ShaderImporter shaderImporter();

        /**
         * @return The set of pre-definitions for shaders
         */
        default ShaderPreDefinitions preDefinitions() {
            return VeilRenderSystem.renderer().getShaderDefinitions();
        }

        /**
         * @return All macros used to compile shaders
         */
        Map<String, String> macros();
    }

    /**
     * Context for modifying source code and shader behavior.
     */
    non-sealed interface MinecraftContext extends Context {

        /**
         * @return The name of the shader instance this was compiled with
         */
        String shaderInstance();

        /**
         * @return The vertex format specified in the shader
         */
        VertexFormat vertexFormat();
    }

    /**
     * Context for modifying source code and shader behavior.
     */
    non-sealed interface VeilContext extends Context {

        /**
         * Sets the uniform binding for a shader.
         *
         * @param name    The name of the uniform
         * @param binding The binding to set it to
         */
        void addUniformBinding(String name, int binding);

        /**
         * @return The definition of the program this is being compiled for or <code>null</code> if the shader is standalone
         */
        @Nullable
        ProgramDefinition definition();
    }

    /**
     * Context for modifying source code and sodium shader behavior.
     */
    non-sealed interface SodiumContext extends Context {
    }

    /**
     * Specifies how includes should interact with existing functions and fields in shader sources.
     *
     * @author Ocelot
     */
    enum IncludeOverloadStrategy {
        /**
         * Will error if any fields or functions from includes conflict with data in shader source files.
         */
        FAIL,
        /**
         * Does not include fields and functions from includes that would conflict with data in shader source files.
         */
        SOURCE,
        /**
         * Replaces the fields and functions shader in source files with data from includes
         */
        INCLUDE
    }
}
