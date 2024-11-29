package foundry.veil.impl.glsl.node.branch;

import com.google.common.collect.Streams;
import foundry.veil.impl.glsl.node.GlslNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * if/else
 *
 * @author Ocelot
 */
public class GlslSelectionNode implements GlslNode {

    private GlslNode expression;
    private final List<GlslNode> first;
    private final List<GlslNode> second;

    public GlslSelectionNode(GlslNode expression, Collection<GlslNode> first, Collection<GlslNode> second) {
        this.expression = expression;
        this.first = new ArrayList<>(first);
        this.second = new ArrayList<>(second);
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
    public Stream<GlslNode> stream() {
        return Streams.concat(Stream.of(this), this.first.stream().flatMap(GlslNode::stream), this.second.stream().flatMap(GlslNode::stream));
    }

    @Override
    public String toString() {
        return "GlslSelectionNode{" +
                "expression=" + this.expression + ", " +
                "first=" + this.first + ", " +
                "branch=" + this.second + '}';
    }
}
