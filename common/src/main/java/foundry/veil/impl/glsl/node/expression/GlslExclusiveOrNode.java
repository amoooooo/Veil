package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.List;

/**
 * Exclusive OR; A ^ B ^ C
 *
 * @param expressions
 * @author Ocelot
 */
public record GlslExclusiveOrNode(List<GlslNode> expressions) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {

    }
}
