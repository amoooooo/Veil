package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.Collection;
import java.util.Collections;

public record GlslUnaryNode(GlslNode expression, Operand operand) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {

    }

    @Override
    public Collection<GlslNode> children() {
        return Collections.singleton(this.expression);
    }

    public enum Operand {
        PRE_INCREMENT, PRE_DECREMENT, POST_INCREMENT, POST_DECREMENT, PLUS, DASH, BANG, TILDE
    }
}
