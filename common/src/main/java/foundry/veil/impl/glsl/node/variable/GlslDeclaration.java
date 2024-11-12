package foundry.veil.impl.glsl.node.variable;

import foundry.veil.impl.glsl.grammar.GlslTypeQualifier;
import foundry.veil.impl.glsl.node.GlslNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
}
