package foundry.veil.impl.glsl.node.variable;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;
import foundry.veil.impl.glsl.grammar.GlslSpecifiedType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record GlslNewNode(GlslSpecifiedType type, String name, @Nullable GlslNode initializer) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {
    }

    @Override
    public Collection<GlslNode> children() {
        return this.initializer != null ? List.of(this.initializer) : Collections.emptyList();
    }
}