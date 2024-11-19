package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;

import java.util.Collection;
import java.util.List;

/**
 * if/else
 *
 * @author Ocelot
 */
public final class GlslSelectionNode implements GlslNode {

    private GlslNode expression;
    private final List<GlslNode> first;
    private final List<GlslNode> second;

    public GlslSelectionNode(GlslNode expression, GlslNode first, GlslNode second) {
        this.expression = expression;
        this.first = first.toList();
        this.second = second.toList();
    }

    public GlslNode getExpression() {
        return this.expression;
    }

    public List<GlslNode> getFirst() {
        return this.first;
    }

    public List<GlslNode> getSecond() {
        return this.second;
    }

    public void setExpression(GlslNode expression) {
        this.expression = expression;
    }

    public GlslSelectionNode setFirst(Collection<GlslNode> first) {
        this.first.clear();
        this.first.addAll(first);
        return this;
    }

    public GlslSelectionNode setSecond(Collection<GlslNode> first) {
        this.first.clear();
        this.first.addAll(first);
        return this;
    }

    @Override
    public String getSourceString() {
        StringBuilder builder = new StringBuilder("if (");
        builder.append(this.expression.getSourceString().replaceAll("\n", "\n\t")).append(") {\n");
        for (GlslNode node : this.first) {
            builder.append('\t').append(node.getSourceString().replaceAll("\n", "\n\t")).append(";\n");
        }
        builder.append("}");
        if (!this.second.isEmpty()) {
            builder.append(" else {\n");
            for (GlslNode node : this.second) {
                builder.append('\t').append(node.getSourceString().replaceAll("\n", "\n\t")).append(";\n");
            }
            builder.append("}");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "GlslSelectionNode{" +
                "expression=" + this.expression + ", " +
                "first=" + this.first + ", " +
                "branch=" + this.second + '}';
    }
}
