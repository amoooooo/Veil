package foundry.veil.api.glsl.node.variable;

import foundry.veil.api.glsl.node.GlslNode;

import java.util.stream.Stream;

public class GlslVariableNode implements GlslNode {

    private String name;

    public GlslVariableNode(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
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
        return "GlslVariableNode{name=" + this.name + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslVariableNode that = (GlslVariableNode) o;
        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
