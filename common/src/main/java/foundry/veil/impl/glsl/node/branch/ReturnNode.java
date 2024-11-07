package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;
import org.jetbrains.annotations.Nullable;

public class ReturnNode implements GlslNode {

    private GlslNode value;

    public ReturnNode(@Nullable GlslNode value) {
        this.value = value;
    }

    @Override
    public void visit(GlslVisitor visitor) {

    }

    public @Nullable GlslNode getValue() {
        return this.value;
    }

    public void setValue(@Nullable GlslNode value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ReturnNode{value=" + this.value + '}';
    }
}
