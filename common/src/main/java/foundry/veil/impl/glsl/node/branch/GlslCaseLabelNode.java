package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

public class GlslCaseLabelNode implements GlslNode {

    private GlslNode condition;

    public GlslCaseLabelNode(@Nullable GlslNode condition) {
        this.condition = condition;
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

    @Override
    public String getSourceString() {
        return "case: " + this.condition.getSourceString();
    }
}
