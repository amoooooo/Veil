package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;
import org.jetbrains.annotations.Nullable;

public class GlslCaseLabelNode implements GlslNode {

    private GlslNode condition;

    public GlslCaseLabelNode(@Nullable GlslNode condition) {
        this.condition = condition;
    }

    @Override
    public void visit(GlslVisitor visitor) {
        visitor.visitCaseLabel(this);
    }

    public boolean isDefault() {
        return this.condition == null;
    }

    public GlslNode getCondition() {
        return this.condition;
    }

    public void setCondition(@Nullable GlslNode condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return "GlslCaseLabelNode{condition=" + (this.condition == null ? "default" : this.condition) + '}';
    }
}
