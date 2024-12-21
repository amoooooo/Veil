package foundry.veil.api.glsl.node.variable;

import foundry.veil.api.glsl.grammar.GlslSpecifiedType;
import foundry.veil.api.glsl.grammar.GlslType;
import foundry.veil.api.glsl.node.GlslNode;
import foundry.veil.api.glsl.node.GlslRootNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public class GlslNewNode implements GlslRootNode {

    private GlslSpecifiedType type;
    private String name;
    private GlslNode initializer;

    public GlslNewNode(GlslType type, @Nullable String name, @Nullable GlslNode initializer) {
        this.type = type.asSpecifiedType();
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public @NotNull GlslSpecifiedType getType() {
        return this.type;
    }

    @Override
    public Stream<GlslNode> stream() {
        return this.initializer != null ? Stream.concat(Stream.of(this), this.initializer.stream()) : Stream.of(this);
    }

    @Override
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

    @Override
    public GlslNewNode setName(@Nullable String name) {
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

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslNewNode that = (GlslNewNode) o;
        return this.type.equals(that.type) && this.name.equals(that.name) && Objects.equals(this.initializer, that.initializer);
    }

    @Override
    public int hashCode() {
        int result = this.type.hashCode();
        result = 31 * result + this.name.hashCode();
        result = 31 * result + Objects.hashCode(this.initializer);
        return result;
    }
}