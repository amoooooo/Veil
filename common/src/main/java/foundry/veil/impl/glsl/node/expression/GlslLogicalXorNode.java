package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ocelot
 */
public class GlslLogicalXorNode implements GlslNode {

    private final List<GlslNode> expressions;

    public GlslLogicalXorNode(Collection<GlslNode> expressions) {
        this.expressions = new ArrayList<>(expressions);
    }

    @Override
    public String getSourceString() {
        return this.expressions.stream().map(GlslNode::getSourceString).collect(Collectors.joining(" ^^ "));
    }

    public List<GlslNode> expressions() {
        return this.expressions;
    }

    @Override
    public String toString() {
        return "GlslLogicalXorNode[expressions=" + this.expressions + ']';
    }
}
