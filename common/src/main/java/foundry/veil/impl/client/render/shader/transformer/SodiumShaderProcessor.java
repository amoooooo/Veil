package foundry.veil.impl.client.render.shader.transformer;

import foundry.veil.VeilClient;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.ShaderImporter;
import foundry.veil.api.client.render.shader.processor.ShaderModifyProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderProcessorList;
import foundry.veil.api.glsl.GlslParser;
import foundry.veil.api.glsl.GlslSyntaxException;
import foundry.veil.api.glsl.node.GlslTree;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferProcessor;
import foundry.veil.impl.compat.sodium.SodiumShaderPreProcessor;
import foundry.veil.lib.anarres.cpp.LexerException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows vanilla and sodium shaders to use shader modifications.
 */
@ApiStatus.Internal
public class SodiumShaderProcessor {

    private static final ThreadLocal<ShaderProcessorList> PROCESSOR = new ThreadLocal<>();

    public static void setup(ResourceProvider provider) {
        ShaderProcessorList list = new ShaderProcessorList(provider);
        list.addPreprocessor(new ShaderModifyProcessor(), false);
        list.addPreprocessor(new DynamicBufferProcessor(), false);
        list.addPreprocessor(new SodiumShaderPreProcessor());
        VeilClient.clientPlatform().onRegisterShaderPreProcessors(provider, list);
        PROCESSOR.set(list);
    }

    public static void free() {
        PROCESSOR.remove();
    }

    public static String modify(@Nullable ResourceLocation name, int activeBuffers, int type, String source) throws IOException, GlslSyntaxException, LexerException {
        ShaderProcessorList processor = PROCESSOR.get();
        if (processor == null) {
            throw new NullPointerException("Processor not initialized");
        }

        processor.getShaderImporter().reset();
        Map<String, String> macros = new HashMap<>();
        DynamicBufferType.addMacros(activeBuffers, macros);
        GlslTree tree = GlslParser.preprocessParse(source, macros);
        processor.getProcessor().modify(new Context(processor, name, activeBuffers, type, macros), tree);
        GlslTree.stripGLMacros(macros);
        tree.getMacros().putAll(macros);
        return tree.toSourceString();
    }

    private record Context(ShaderProcessorList processor,
                           ResourceLocation name,
                           int activeBuffers,
                           int type,
                           Map<String, String> macros) implements ShaderPreProcessor.SodiumContext {

        @Override
        public GlslTree modifyInclude(@Nullable ResourceLocation name, String source) throws IOException, GlslSyntaxException, LexerException {
            GlslTree tree = GlslParser.preprocessParse(source, this.macros);
            this.processor.getImportProcessor().modify(new Context(this.processor, name, this.activeBuffers, this.type, this.macros), tree);
            return tree;
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
        public ShaderImporter shaderImporter() {
            return this.processor.getShaderImporter();
        }
    }
}
