package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.visitor.GlslVisitor;

public enum GlslEmptyNode implements GlslNode {
    INSTANCE;

    @Override
    public void visit(GlslVisitor visitor) {
    }
}
