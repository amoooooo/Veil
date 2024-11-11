package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslTreeVisitor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exclusive OR; A ^ B ^ C
 *
 * @param expressions
 * @author Ocelot
 */
public record GlslExclusiveOrNode(List<GlslNode> expressions) implements GlslNode {

    @Override
    public String getSourceString() {
        return this.expressions.stream().map(GlslNode::getSourceString).collect(Collectors.joining(" ^ "));
    }
}
