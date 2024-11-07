package foundry.veil.impl.glsl.node.variable;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.Collection;
import java.util.List;

public record GlslArrayNode(GlslNode expression, GlslNode index) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {
    }

    @Override
    public Collection<GlslNode> children() {
        return List.of(this.expression, this.index);
    }
}
