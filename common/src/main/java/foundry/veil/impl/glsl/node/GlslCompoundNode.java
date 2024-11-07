package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.List;

public record GlslCompoundNode(List<GlslNode> children) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {

    }
}
