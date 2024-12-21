package foundry.veil.api.glsl.node.function;

import foundry.veil.api.glsl.node.GlslNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GlslInvokeFunctionNode implements GlslNode {

    private GlslNode header;
    private final List<GlslNode> parameters;

    public GlslInvokeFunctionNode(GlslNode header, Collection<GlslNode> parameters) {
        this.header = header;
        this.parameters = new ArrayList<>(parameters);
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
        return "GlslInvokeFunctionNode{name=" + this.header + ", parameters=" + this.parameters + '}';
    }

    @Override
    public String getSourceString() {
        String parameters = this.parameters.stream().map(GlslNode::getSourceString).collect(Collectors.joining(", "));
        return this.header.getSourceString() + "(" + parameters + ")";
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.concat(Stream.of(this), this.parameters.stream().flatMap(GlslNode::stream));
    }
}
