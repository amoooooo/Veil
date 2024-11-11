package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

public class GlslReturnNode implements GlslNode {

    private GlslNode value;

    public GlslReturnNode(@Nullable GlslNode value) {
        this.value = value;
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

    @Override
    public String getSourceString() {
        return this.value != null ? "return " + this.value.getSourceString() : "return";
    }
}
