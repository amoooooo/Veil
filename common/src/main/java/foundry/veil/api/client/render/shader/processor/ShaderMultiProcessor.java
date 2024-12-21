package foundry.veil.api.client.render.shader.processor;

import foundry.veil.api.glsl.GlslSyntaxException;
import foundry.veil.api.glsl.node.GlslTree;
import foundry.veil.lib.anarres.cpp.LexerException;

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
