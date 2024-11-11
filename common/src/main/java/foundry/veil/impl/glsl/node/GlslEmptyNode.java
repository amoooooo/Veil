package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.visitor.GlslNodeVisitor;

import java.util.ArrayList;
import java.util.List;

public enum GlslEmptyNode implements GlslNode {
    INSTANCE;

    @Override
    public String getSourceString() {
        return "";
    }

    @Override
    public List<GlslNode> toList() {
        return new ArrayList<>();
    }

    @Override
    public void visit(GlslNodeVisitor visitor) {
    }
}
