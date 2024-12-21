package foundry.veil.api.client.render.shader.processor;

import foundry.veil.impl.glsl.GlslSyntaxException;
import foundry.veil.impl.glsl.grammar.GlslVersionStatement;
import foundry.veil.impl.glsl.node.GlslTree;
import foundry.veil.lib.anarres.cpp.LexerException;

import java.io.IOException;

/**
 * Adds the version and required extensions for all shaders that do not define a version.
 *
 * @author Ocelot
 */
public class ShaderVersionProcessor implements ShaderPreProcessor {

    @Override
    public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
        GlslVersionStatement version = tree.getVersionStatement();
        if (version.getVersion() == 110 && version.isCore()) {
            version.setVersion(410);
        }
    }
}
