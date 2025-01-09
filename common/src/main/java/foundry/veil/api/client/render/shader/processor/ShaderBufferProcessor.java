package foundry.veil.api.client.render.shader.processor;

import foundry.veil.api.client.registry.VeilShaderBufferRegistry;
import foundry.veil.api.client.render.VeilShaderBufferLayout;
import foundry.veil.api.glsl.GlslSyntaxException;
import foundry.veil.api.glsl.node.GlslTree;
import foundry.veil.lib.anarres.cpp.LexerException;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Processes a shader to add buffer bindings.
 *
 * @author Ocelot
 */
public class ShaderBufferProcessor implements ShaderPreProcessor {

    private static final String BUFFER_KEY = "#veil:buffer ";
    private final boolean shaderStorageSupported;

    public ShaderBufferProcessor(boolean shaderStorageSupported) {
        this.shaderStorageSupported = shaderStorageSupported;
    }

    @Override
    public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
        List<String> buffers = new ArrayList<>();
        List<String> directives = tree.getDirectives();
        for (String directive : directives) {
            if (directive.startsWith(ShaderBufferProcessor.BUFFER_KEY)) {
                buffers.add(directive);
            }
        }
        for (String directive : buffers) {
            String[] parts = directive.substring(ShaderBufferProcessor.BUFFER_KEY.length()).split(" +", 2);
            String bufferId = parts[0].trim();
            String interfaceName = parts.length > 1 ? parts[1].trim() : null;

            try {
                ResourceLocation name = ResourceLocation.parse(bufferId);
                VeilShaderBufferLayout<?> layout = VeilShaderBufferRegistry.REGISTRY.get(name);
                if (layout == null) {
                    throw new IOException("Unknown buffer: " + name);
                }

                GlslTree loadedImport = new GlslTree();
                loadedImport.getBody().add(layout.createNode(this.shaderStorageSupported, interfaceName));
                ctx.include(tree, "#buffer " + name, loadedImport, IncludeOverloadStrategy.SOURCE);
            } catch (ResourceLocationException e) {
                throw new IOException("Invalid buffer: " + bufferId, e);
            }
        }
        directives.removeAll(buffers);
    }
}
