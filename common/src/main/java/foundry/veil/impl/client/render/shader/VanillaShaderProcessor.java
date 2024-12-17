package foundry.veil.impl.client.render.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.VeilClient;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.ShaderProcessorList;
import foundry.veil.api.client.render.shader.processor.ShaderCustomProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderModifyProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferProcessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Allows vanilla and sodium shaders to use shader modifications.
 */
@ApiStatus.Internal
public class VanillaShaderProcessor {

    private static final ThreadLocal<ShaderProcessorList> PROCESSOR = new ThreadLocal<>();

    public static void setup(ResourceProvider provider, int activeBuffers) {
        ShaderProcessorList list = new ShaderProcessorList();
        list.addPreprocessor(new ShaderModifyProcessor());
        list.addPreprocessor(new ShaderCustomProcessor(provider));
        list.addPreprocessor(new DynamicBufferProcessor(DynamicBufferType.decode(activeBuffers)));
        VeilClient.clientPlatform().onRegisterShaderPreProcessors(provider, list);
        PROCESSOR.set(list);
    }

    public static void free() {
        PROCESSOR.remove();
    }

    public static String modify(@Nullable String shaderInstance, @Nullable ResourceLocation name, @Nullable VertexFormat vertexFormat, int type, String source) throws IOException {
        ShaderProcessorList processor = PROCESSOR.get();
        if (processor == null) {
            throw new NullPointerException("Processor not initialized");
        }
        return processor.getProcessor().modify(new Context(shaderInstance, name, type, vertexFormat), source);
    }

    private record Context(String shaderInstance, ResourceLocation name, int type,
                           VertexFormat vertexFormat) implements ShaderPreProcessor.MinecraftContext {

        @Override
        public String modify(@Nullable ResourceLocation name, String source) throws IOException {
            return PROCESSOR.get().getImportProcessor().modify(new Context(this.shaderInstance, name, this.type, this.vertexFormat), source);
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
        public Set<ResourceLocation> includes() {
            return Collections.emptySet();
        }
    }
}
