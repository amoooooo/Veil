package foundry.veil.api.client.render.shader.processor;

import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.lib.anarres.cpp.LexerException;

import java.io.IOException;

public record ShaderMultiProcessor(ShaderPreProcessor[] processors) implements ShaderPreProcessor {

    @Override
    public void prepare() {
        for (ShaderPreProcessor processor : this.processors) {
            processor.prepare();
        }
    }

    @Override
    public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
        for (ShaderPreProcessor preProcessor : this.processors) {
            preProcessor.modify(ctx, tree);
        }
    }
}
