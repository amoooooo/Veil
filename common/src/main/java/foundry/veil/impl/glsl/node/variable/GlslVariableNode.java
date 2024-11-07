package foundry.veil.impl.glsl.node.variable;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

public record GlslVariableNode(String name) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {

    }
}
