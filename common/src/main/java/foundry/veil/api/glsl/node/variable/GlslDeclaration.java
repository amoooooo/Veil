package foundry.veil.api.glsl.node.variable;

import foundry.veil.api.glsl.grammar.GlslTypeQualifier;
import foundry.veil.api.glsl.node.GlslNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class GlslDeclaration implements GlslNode {

    private final List<GlslTypeQualifier> typeQualifiers;
    private final List<String> names;

    public GlslDeclaration(Collection<GlslTypeQualifier> typeQualifiers, Collection<String> names) {
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
}
