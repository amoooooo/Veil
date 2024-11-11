package foundry.veil.impl.glsl.node.variable;

import foundry.veil.impl.glsl.node.GlslNode;

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
    public String toString() {
        return "GlslVariableNode{" +
                "name=" + this.name + '}';
    }
}
