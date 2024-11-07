package foundry.veil.impl.glsl.node.variable;

import foundry.veil.impl.glsl.grammar.GlslType;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;
import foundry.veil.impl.glsl.grammar.GlslSpecifiedType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GlslNewNode implements GlslNode {

    private GlslSpecifiedType type;
    private String name;
    private GlslNode initializer;

    public GlslNewNode(GlslType type, String name, @Nullable GlslNode initializer) {
        this.type = type.asSpecifiedType();
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public void visit(GlslVisitor visitor) {
    }

    @Override
    public Collection<GlslNode> children() {
        return this.initializer != null ? List.of(this.initializer) : Collections.emptyList();
    }

    public GlslSpecifiedType getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public GlslNode getInitializer() {
        return this.initializer;
    }

    public GlslNewNode setType(GlslType type) {
        this.type = type.asSpecifiedType();
        return this;
    }

    public GlslNewNode setName(String name) {
        this.name = name;
        return this;
    }

    public GlslNewNode setInitializer(@Nullable GlslNode initializer) {
        this.initializer = initializer;
        return this;
    }

    @Override
    public String toString() {
        return "GlslNewNode{type=" + this.type + ", name='" + this.name + "', initializer=" + this.initializer + '}';
    }
}