package foundry.veil.api.client.render.shader.processor;

import foundry.veil.api.glsl.GlslSyntaxException;
import foundry.veil.api.glsl.node.GlslTree;
import foundry.veil.lib.anarres.cpp.LexerException;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Processes a shader to add imports.
 *
 * @author Ocelot
 */
public class ShaderImportProcessor implements ShaderPreProcessor {

    private static final String INCLUDE_KEY = "#include ";

    @Override
    public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
        List<String> imports = new ArrayList<>();
        List<String> directives = tree.getDirectives();
        for (String directive : directives) {
            if (directive.startsWith(ShaderImportProcessor.INCLUDE_KEY)) {
                imports.add(directive);
            }
        }
        for (String directive : imports) {
            String importId = directive.substring(ShaderImportProcessor.INCLUDE_KEY.length()).trim();
            try {
                ctx.include(tree, ResourceLocation.parse(importId));
            } catch (ResourceLocationException e) {
                throw new IOException("Invalid import: " + importId, e);
            }
        }
        directives.removeAll(imports);
    }
}
