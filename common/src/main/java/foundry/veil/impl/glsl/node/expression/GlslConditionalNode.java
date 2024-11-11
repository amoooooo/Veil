package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;

/**
 * ?:
 *
 * @author Ocelot
 */
public class GlslConditionalNode implements GlslNode {

    private  GlslNode condition;
    private  GlslNode first;
    private  GlslNode second;

    public GlslConditionalNode(GlslNode condition, GlslNode first, GlslNode second) {
        this.condition = condition;
        this.first = first;
        this.second = second;
    }

    public GlslNode getCondition() {
        return this.condition;
    }

    public GlslNode getFirst() {
        return this.first;
    }

    public GlslNode getSecond() {
        return this.second;
    }

    public GlslConditionalNode setCondition(GlslNode condition) {
        this.condition = condition;
        return this;
    }

    public GlslConditionalNode setFirst(GlslNode first) {
        this.first = first;
        return this;
    }

    public GlslConditionalNode setSecond(GlslNode second) {
        this.second = second;
        return this;
    }

    @Override
    public String getSourceString() {
        return this.condition.getSourceString() + " " + this.first.getSourceString() + " : " + this.second.getSourceString();
    }

    @Override
    public String toString() {
        return "GlslConditionalNode{" +
                "condition=" + this.condition + ", " +
                "first=" + this.first + ", " +
                "second=" + this.second + '}';
    }
}
