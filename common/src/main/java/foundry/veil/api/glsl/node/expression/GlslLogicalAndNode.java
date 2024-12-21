package foundry.veil.api.glsl.node.expression;

import foundry.veil.api.glsl.node.GlslNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ocelot
 */
public class GlslLogicalAndNode implements GlslNode {

    private final List<GlslNode> expressions;

    public GlslLogicalAndNode(Collection<GlslNode> expressions) {
        this.expressions = new ArrayList<>(expressions);
    }

    public List<GlslNode> getExpressions() {
        return this.expressions;
    }

    @Override
    public String getSourceString() {
        return '(' + this.expressions.stream().map(GlslNode::getSourceString).collect(Collectors.joining(" && ")) + ')';
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.concat(Stream.of(this), this.expressions.stream().flatMap(GlslNode::stream));
    }

    @Override
    public String toString() {
        return "GlslLogicalAndNode[expressions=" + this.expressions + ']';
    }
}
