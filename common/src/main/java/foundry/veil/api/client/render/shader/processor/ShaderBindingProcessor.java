package foundry.veil.api.client.render.shader.processor;

import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.grammar.GlslSpecifiedType;
import io.github.ocelot.glslprocessor.api.grammar.GlslTypeQualifier;
import io.github.ocelot.glslprocessor.api.grammar.GlslTypeSpecifier;
import io.github.ocelot.glslprocessor.api.node.GlslConstantNode;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.lib.anarres.cpp.LexerException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Adds support for <code>layout(binding = #)</code> in the shader source without needing shader version 420.
 *
 * @author Ocelot
 */
public class ShaderBindingProcessor implements ShaderPreProcessor {

    @Override
    public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
        if (tree.getVersionStatement().getVersion() >= 420) {
            return;
        }

        if (!(ctx instanceof VeilContext veilContext)) {
            return;
        }

        tree.fields().forEach(node -> {
            GlslSpecifiedType type = node.getType();
            // Ignore atomic counters
            if (type.getSpecifier() == GlslTypeSpecifier.BuiltinType.ATOMIC_UINT) {
                return;
            }

            Iterator<GlslTypeQualifier> qualifierIterator = type.getQualifiers().iterator();
            while (qualifierIterator.hasNext()) {
                GlslTypeQualifier qualifier = qualifierIterator.next();
                if (qualifier instanceof GlslTypeQualifier.Layout(List<GlslTypeQualifier.LayoutId> layoutIds)) {
                    Iterator<GlslTypeQualifier.LayoutId> layoutIdIterator = layoutIds.iterator();
                    while (layoutIdIterator.hasNext()) {
                        GlslTypeQualifier.LayoutId layoutId = layoutIdIterator.next();
                        if ("binding".equals(layoutId.identifier()) && layoutId.expression() instanceof GlslConstantNode constantNode) {
                            veilContext.addUniformBinding(Objects.requireNonNullElse(node.getName(), type.getSourceString()), constantNode.intValue());
                            layoutIdIterator.remove();
                        }
                    }
                    if (layoutIds.isEmpty()) {
                        qualifierIterator.remove();
                    }
                }
            }
        });
    }
}
