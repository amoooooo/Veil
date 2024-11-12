package foundry.veil.impl.glsl.grammar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class GlslStructSpecifier implements GlslTypeSpecifier {

    private String name;
    private final List<GlslStructField> fields;

    public GlslStructSpecifier(String name, Collection<GlslStructField> fields) {
        this.name = name;
        this.fields = new ArrayList<>(fields);
    }

    public String getName() {
        return this.name;
    }

    public List<GlslStructField> getFields() {
        return this.fields;
    }

    public GlslStructSpecifier setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getSourceString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.name).append(" {\n");
        for (GlslStructField field : this.fields) {
            builder.append('\t').append(field.getSourceString().replaceAll("\n", "\n\t")).append(";\n");
        }
        builder.append('}');
        return builder.toString();
    }

    @Override
    public String toString() {
        return "GlslStructSpecifier{name=" + this.name + '}';
    }
}
