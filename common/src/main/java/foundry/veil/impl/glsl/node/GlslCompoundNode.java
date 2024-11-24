package foundry.veil.impl.glsl.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class GlslCompoundNode implements GlslNode {

    private final List<GlslNode> children;

    public GlslCompoundNode(List<GlslNode> children) {
        this.children = children;
    }

    @Override
    public List<GlslNode> toList() {
        return new ArrayList<>(this.children);
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.concat(Stream.of(this), this.children.stream().flatMap(GlslNode::stream));
    }

    public List<GlslNode> getChildren() {
        return this.children;
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
            builder.append('\t').append(child.getSourceString().replaceAll("\n", "\n\t")).append(";\n");
        }
        builder.append('}');
        return builder.toString();
    }

    @Override
    public String toString() {
        return "GlslCompoundNode{children=" + this.children + '}';
    }
}
