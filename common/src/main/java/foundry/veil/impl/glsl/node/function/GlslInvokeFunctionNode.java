package foundry.veil.impl.glsl.node.function;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GlslInvokeFunctionNode implements GlslNode {

    private GlslNode header;
    private final List<GlslNode> parameters;

    public GlslInvokeFunctionNode(GlslNode header, Collection<GlslNode> parameters) {
        this.header = header;
        this.parameters = new ArrayList<>(parameters);
    }

    @Override
    public void visit(GlslVisitor visitor) {

    }

    public GlslNode getHeader() {
        return this.header;
    }

    public List<GlslNode> getParameters() {
        return this.parameters;
    }

    public void setHeader(GlslNode header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return "GlslInvokeFunctionNode{header=" + this.header + ", parameters=" + this.parameters + '}';
    }
}
