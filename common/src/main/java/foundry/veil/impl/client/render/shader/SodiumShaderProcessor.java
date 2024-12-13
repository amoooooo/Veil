package foundry.veil.impl.client.render.shader;

import foundry.veil.VeilClient;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.ShaderProcessorList;
import foundry.veil.api.client.render.shader.processor.ShaderCustomProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderModifyProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferProcessor;
import foundry.veil.impl.compat.SodiumShaderPreProcessor;
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
public class SodiumShaderProcessor {

    private static final ThreadLocal<ShaderProcessorList> PROCESSOR = new ThreadLocal<>();

    public static void setup(ResourceProvider resourceProvider, int activeBuffers) {
        ShaderProcessorList list = new ShaderProcessorList();
        list.addPreprocessor(new ShaderModifyProcessor());
        list.addPreprocessor(new ShaderCustomProcessor(resourceProvider));
        list.addPreprocessor(new DynamicBufferProcessor(DynamicBufferType.decode(activeBuffers)));
        list.addPreprocessor(new SodiumShaderPreProcessor());
        VeilClient.clientPlatform().onRegisterShaderPreProcessors(list);
        PROCESSOR.set(list);
    }

    public static void free() {
        PROCESSOR.remove();
    }

    public static String modify(@Nullable ResourceLocation name, int type, String source) throws IOException {
        ShaderProcessorList processor = PROCESSOR.get();
        if (processor == null) {
            throw new NullPointerException("Processor not initialized");
        }
        return processor.getProcessor().modify(new Context(name, type), source);
    }

    private record Context(ResourceLocation name, int type) implements ShaderPreProcessor.SodiumContext {

        @Override
        public String modify(@Nullable ResourceLocation name, String source) throws IOException {
            return PROCESSOR.get().getImportProcessor().modify(new Context(name, this.type), source);
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
