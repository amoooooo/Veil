package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;
import org.jetbrains.annotations.Nullable;

/**
 * ?:
 *
 * @param condition
 * @param first
 * @param branch
 * @author Ocelot
 */
public record GlslConditionalNode(GlslNode condition, GlslNode first, @Nullable GlslNode branch) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {

    }
}
