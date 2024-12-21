package foundry.veil.impl.client.render.shader.transformer;

import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.VeilClient;
import foundry.veil.api.client.render.shader.ShaderImporter;
import foundry.veil.api.client.render.shader.processor.ShaderModifyProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderProcessorList;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferProcessor;
import foundry.veil.api.glsl.GlslParser;
import foundry.veil.api.glsl.GlslSyntaxException;
import foundry.veil.api.glsl.node.GlslTree;
import foundry.veil.lib.anarres.cpp.LexerException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;

/**
 * Allows vanilla and sodium shaders to use shader modifications.
 */
@ApiStatus.Internal
public class VanillaShaderProcessor {

    private static final ThreadLocal<ShaderProcessorList> PROCESSOR = new ThreadLocal<>();

    public static void setup(ResourceProvider provider) {
        ShaderProcessorList list = new ShaderProcessorList(provider);
        list.addPreprocessor(new ShaderModifyProcessor(), false);
        list.addPreprocessor(new DynamicBufferProcessor(), false);
        VeilClient.clientPlatform().onRegisterShaderPreProcessors(provider, list);
        PROCESSOR.set(list);
    }

    public static void free() {
        PROCESSOR.remove();
    }

    public static String modify(@Nullable String shaderInstance, @Nullable ResourceLocation name, @Nullable VertexFormat vertexFormat, int activeBuffers, int type, String source) throws IOException, GlslSyntaxException, LexerException {
        ShaderProcessorList processor = PROCESSOR.get();
        if (processor == null) {
            throw new NullPointerException("Processor not initialized");
        }

        processor.getShaderImporter().reset();
        GlslTree tree = GlslParser.preprocessParse(source, Collections.emptyMap());
        processor.getProcessor().modify(new Context(processor, shaderInstance, name, activeBuffers, type, vertexFormat), tree);
        return tree.toSourceString();
    }

    private record Context(ShaderProcessorList processor,
                           String shaderInstance,
                           ResourceLocation name,
                           int activeBuffers,
                           int type,
                           VertexFormat vertexFormat) implements ShaderPreProcessor.MinecraftContext {

        @Override
        public GlslTree modifyInclude(@Nullable ResourceLocation name, String source) throws IOException, GlslSyntaxException, LexerException {
            GlslTree tree = GlslParser.parse(source);
            this.processor.getImportProcessor().modify(new Context(this.processor, this.shaderInstance, name, this.activeBuffers, this.type, this.vertexFormat), tree);
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
