package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

public enum JumpNode implements GlslNode {

    CONTINUE, BREAK, DISCARD;

    @Override
    public void visit(GlslVisitor visitor) {

    }
}
