package foundry.veil.impl.glsl.node.variable;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.Collection;
import java.util.Collections;

public record GlslFieldNode(GlslNode expression, String fieldSelection) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {
    }

    @Override
    public Collection<GlslNode> children() {
        return Collections.singleton(this.expression);
    }
}
