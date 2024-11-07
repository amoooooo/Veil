package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;
import org.jetbrains.annotations.Nullable;

public record GlslCaseLabelNode(@Nullable GlslNode condition) implements GlslNode {

    public boolean isDefault() {
        return this.condition == null;
    }

    @Override
    public void visit(GlslVisitor visitor) {

    }
}
