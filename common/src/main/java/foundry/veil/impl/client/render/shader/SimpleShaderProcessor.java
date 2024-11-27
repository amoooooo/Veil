package foundry.veil.impl.client.render.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.processor.ShaderCustomProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderModifyProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.program.ProgramDefinition;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferProcessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * Allows vanilla and sodium shaders to use shader modifications.
 */
@ApiStatus.Internal
public class SimpleShaderProcessor {

    private static final ThreadLocal<ShaderPreProcessor> PROCESSOR = new ThreadLocal<>();

    public static void setup(ResourceProvider resourceProvider, Collection<ShaderPreProcessor> additional) {
        int activeBuffers = VeilRenderSystem.renderer().getDynamicBufferManger().getActiveBuffers();
        List<ShaderPreProcessor> processors = new ArrayList<>();
        processors.add(new ShaderModifyProcessor());
        processors.add(new ShaderCustomProcessor(resourceProvider));
        processors.add(new DynamicBufferProcessor(DynamicBufferType.decode(activeBuffers)));
        processors.addAll(additional);
        SimpleShaderProcessor.PROCESSOR.set(ShaderPreProcessor.allOf(processors));
    }

    public static void free() {
        SimpleShaderProcessor.PROCESSOR.remove();
    }

    public static String modify(@Nullable String shaderInstance, @Nullable ResourceLocation name, @Nullable VertexFormat vertexFormat, int type, String source) throws IOException {
        ShaderPreProcessor processor = SimpleShaderProcessor.PROCESSOR.get();
        if (processor == null) {
            throw new NullPointerException("Processor not initialized");
        }
        return processor.modify(new Context(shaderInstance, name, type, vertexFormat), source);
    }

    private record Context(String shaderInstance, ResourceLocation name, int type,
                           VertexFormat vertexFormat) implements ShaderPreProcessor.Context {

        @Override
        public String modify(@Nullable ResourceLocation name, String source) throws IOException {
            return SimpleShaderProcessor.PROCESSOR.get().modify(new Context(this.shaderInstance, name, this.type, this.vertexFormat), source);
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
        public ShaderPreDefinitions preDefinitions() {
            return VeilRenderSystem.renderer().getShaderDefinitions();
        }
    }
}
