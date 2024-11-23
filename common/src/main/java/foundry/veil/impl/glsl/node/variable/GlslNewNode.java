package foundry.veil.impl.glsl.node.variable;

import foundry.veil.impl.glsl.grammar.GlslSpecifiedType;
import foundry.veil.impl.glsl.grammar.GlslType;
import foundry.veil.impl.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

public class GlslNewNode implements GlslNode {

    private GlslSpecifiedType type;
    private String name;
    private GlslNode initializer;

    public GlslNewNode(GlslType type, @Nullable String name, @Nullable GlslNode initializer) {
        this.type = type.asSpecifiedType();
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public GlslSpecifiedType getType() {
        return this.type;
    }

    public @Nullable String getName() {
        return this.name;
    }

    public @Nullable GlslNode getInitializer() {
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
    public String getSourceString() {
        StringBuilder builder = new StringBuilder(this.type.getSourceString());
        if (this.name != null) {
            builder.append(' ').append(this.name);
            builder.append(this.type.getPostSourceString());
            if (this.initializer != null) {
                builder.append(" = ").append(this.initializer.getSourceString());
            }
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "GlslNewNode{operand=" + this.type + ", name='" + this.name + "', initializer=" + this.initializer + '}';
    }
}