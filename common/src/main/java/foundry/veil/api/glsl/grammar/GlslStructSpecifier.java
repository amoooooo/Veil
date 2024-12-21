package foundry.veil.api.glsl.grammar;

import foundry.veil.api.glsl.node.GlslNode;

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
            builder.append('\t').append(GlslNode.NEWLINE.matcher(field.getSourceString()).replaceAll("\n\t")).append(";\n");
        }
        builder.append('}');
        return builder.toString();
    }

    @Override
    public boolean isStruct() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslStructSpecifier that = (GlslStructSpecifier) o;
        return this.name.equals(that.name) && this.fields.equals(that.fields);
    }

    @Override
    public int hashCode() {
        int result = this.name.hashCode();
        result = 31 * result + this.fields.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "GlslStructSpecifier{name=" + this.name + '}';
    }
}
