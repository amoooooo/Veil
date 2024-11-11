package foundry.veil.impl.glsl.node.variable;

import foundry.veil.impl.glsl.node.GlslNode;

public class GlslArrayNode implements GlslNode {

    private GlslNode expression;
    private GlslNode index;

    public GlslArrayNode(GlslNode expression, GlslNode index) {
        this.expression = expression;
        this.index = index;
    }

    public GlslNode getExpression() {
        return this.expression;
    }

    public GlslNode getIndex() {
        return this.index;
    }

    public GlslArrayNode setExpression(GlslNode expression) {
        this.expression = expression;
        return this;
    }

    public GlslArrayNode setIndex(GlslNode index) {
        this.index = index;
        return this;
    }

    @Override
    public String getSourceString() {
        return this.expression.getSourceString() + '[' + this.index.getSourceString() + ']';
    }

    @Override
    public String toString() {
        return "GlslArrayNode{" +
                "expression=" + this.expression + ", " +
                "index=" + this.index + '}';
    }
}
