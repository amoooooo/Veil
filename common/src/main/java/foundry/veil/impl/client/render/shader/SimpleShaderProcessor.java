package foundry.veil.impl.client.render.shader;

import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.processor.ShaderCustomProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderModifyProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Allows vanilla and sodium shaders to use shader modifications.
 */
@ApiStatus.Internal
public class SimpleShaderProcessor {

    private static ShaderPreProcessor processor;

    public static void setup(ResourceProvider resourceProvider) {
        processor = ShaderPreProcessor.allOf(new ShaderModifyProcessor(), new ShaderCustomProcessor(resourceProvider));
    }

    public static void free() {
        processor = null;
    }

    public static String modify(@Nullable ResourceLocation name, int type, String source) throws IOException {
        if (processor == null) {
            throw new NullPointerException("Processor not initialized");
        }
        return processor.modify(new Context(name, type), source);
    }

    private record Context(ResourceLocation name, int type) implements ShaderPreProcessor.Context {

        @Override
        public String modify(@Nullable ResourceLocation name, String source) throws IOException {
            return processor.modify(new Context(name, this.type), source);
        }

        @Override
        public void addUniformBinding(String name, int binding) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addDefinitionDependency(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addInclude(ResourceLocation name) {
        }

        @Override
        public Set<ResourceLocation> includes() {
            return Collections.emptySet();
        }

        @Override
        public @Nullable ResourceLocation name() {
            return this.name;
        }

        @Override
        public boolean isSourceFile() {
            return true;
        }

        @Override
        public @Nullable ProgramDefinition definition() {
            return null;
        }

        @Override
        public @Nullable ShaderPreDefinitions preDefinitions() {
            return null;
        }
    }
}
