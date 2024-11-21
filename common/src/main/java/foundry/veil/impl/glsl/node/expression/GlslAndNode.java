package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ocelot
 */
public class GlslAndNode implements GlslNode {

    private final List<GlslNode> expressions;

    public GlslAndNode(List<GlslNode> expressions) {
        this.expressions = expressions;
    }

    public List<GlslNode> getExpressions() {
        return this.expressions;
    }

    public GlslAndNode setExpressions(Collection<GlslNode> expressions) {
        this.expressions.clear();
        this.expressions.addAll(expressions);
        return this;
    }

    public GlslAndNode setExpressions(GlslNode... expressions) {
        this.expressions.clear();
        this.expressions.addAll(Arrays.asList(expressions));
        return this;
    }

    @Override
    public String getSourceString() {
        return this.expressions.stream().map(GlslNode::getSourceString).collect(Collectors.joining(" & "));
    }

    @Override
    public String toString() {
        return "GlslAndNode[expressions=" + this.expressions + ']';
    }
}
