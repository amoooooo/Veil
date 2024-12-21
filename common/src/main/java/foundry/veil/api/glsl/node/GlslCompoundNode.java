package foundry.veil.api.glsl.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public record GlslCompoundNode(List<GlslNode> children) implements GlslNode {

    @Override
    public List<GlslNode> toList() {
        return new ArrayList<>(this.children);
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.concat(Stream.of(this), this.children.stream().flatMap(GlslNode::stream));
    }

    public GlslCompoundNode setChildren(Collection<GlslNode> children) {
        this.children.clear();
        this.children.addAll(children);
        return this;
    }

    public GlslCompoundNode setChildren(GlslNode... children) {
        this.children.clear();
        this.children.addAll(Arrays.asList(children));
        return this;
    }

    @Override
    public String getSourceString() {
        StringBuilder builder = new StringBuilder("{\n");
        for (GlslNode child : this.children) {
            builder.append('\t').append(NEWLINE.matcher(child.getSourceString()).replaceAll("\n\t")).append(";\n");
        }
        builder.append('}');
        return builder.toString();
    }

    @Override
    public String toString() {
        return "GlslCompoundNode{children=" + this.children + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslCompoundNode that = (GlslCompoundNode) o;
        return this.children.equals(that.children);
    }

    @Override
    public int hashCode() {
        return this.children.hashCode();
    }
}
