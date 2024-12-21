package foundry.veil.api.glsl.node.variable;

import foundry.veil.api.glsl.node.GlslNode;

import java.util.stream.Stream;

public class GlslVariableNode implements GlslNode {

    private String name;

    public GlslVariableNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public GlslVariableNode setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getSourceString() {
        return this.name;
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.of(this);
    }

    @Override
    public String toString() {
        return "GlslVariableNode{" +
                "name=" + this.name + '}';
    }
}
