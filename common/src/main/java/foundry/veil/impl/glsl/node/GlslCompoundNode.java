package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.visitor.GlslNodeVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
    public void visit(GlslNodeVisitor visitor) {
        for (GlslNode child : this.children) {
            child.visit(visitor);
        }
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
        StringBuilder builder = new StringBuilder();
        for (GlslNode child : this.children) {
            builder.append(child.getSourceString()).append('\n');
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    @Override
    public String toString() {
        return "GlslCompoundNode{children=" + this.children + '}';
    }
}
