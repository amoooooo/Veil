package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.List;

/**
 * Inclusive OR; A | B | C
 *
 * @param expressions
 * @author Ocelot
 */
public record GlslInclusiveOrNode(List<GlslNode> expressions) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {

    }
}
