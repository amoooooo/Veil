package foundry.veil.api.glsl.node.variable;

import foundry.veil.api.glsl.grammar.GlslTypeQualifier;
import foundry.veil.api.glsl.node.GlslNode;
import foundry.veil.api.glsl.node.GlslRootNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class GlslDeclarationNode implements GlslRootNode {

    private final List<GlslTypeQualifier> typeQualifiers;
    private final List<String> names;

    public GlslDeclarationNode(Collection<GlslTypeQualifier> typeQualifiers, Collection<String> names) {
        this.typeQualifiers = new ArrayList<>(typeQualifiers);
        this.names = new ArrayList<>(names);
    }

    public List<GlslTypeQualifier> getTypeQualifiers() {
        return this.typeQualifiers;
    }

    public List<String> getNames() {
        return this.names;
    }

    @Override
    public String getSourceString() {
        StringBuilder builder = new StringBuilder();
        for (GlslTypeQualifier qualifier : this.typeQualifiers) {
            builder.append(qualifier.getSourceString()).append(' ');
        }
        for (String name : this.names) {
            builder.append(name).append(", ");
        }
        if (!this.names.isEmpty()) {
            builder.delete(builder.length() - 2, builder.length());
        }
        return builder.toString().trim();
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.of(this);
    }

    @Override
    public @Nullable String getName() {
        throw new UnsupportedOperationException("Use getNames() instead");
    }

    @Override
    public GlslRootNode setName(@Nullable String name) {
        throw new UnsupportedOperationException("Use getNames() instead");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslDeclarationNode that = (GlslDeclarationNode) o;
        return this.typeQualifiers.equals(that.typeQualifiers) && this.names.equals(that.names);
    }

    @Override
    public int hashCode() {
        int result = this.typeQualifiers.hashCode();
        result = 31 * result + this.names.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "GlslDeclarationNode{typeQualifiers=" + this.typeQualifiers + ", names=" + this.names + '}';
    }
}
