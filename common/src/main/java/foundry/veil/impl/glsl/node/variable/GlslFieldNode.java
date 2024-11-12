package foundry.veil.impl.glsl.node.variable;

import foundry.veil.impl.glsl.node.GlslNode;

public class GlslFieldNode implements GlslNode {

    private GlslNode expression;
    private String fieldSelection;

    public GlslFieldNode(GlslNode expression, String fieldSelection) {
        this.expression = expression;
        this.fieldSelection = fieldSelection;
    }

    public GlslNode getExpression() {
        return this.expression;
    }

    public String getFieldSelection() {
        return this.fieldSelection;
    }

    public GlslFieldNode setExpression(GlslNode expression) {
        this.expression = expression;
        return this;
    }

    public GlslFieldNode setFieldSelection(String fieldSelection) {
        this.fieldSelection = fieldSelection;
        return this;
    }

    @Override
    public String getSourceString() {
        if (this.expression instanceof GlslVariableNode || this.expression instanceof GlslFieldNode) {
            return this.expression.getSourceString() + '.' + this.fieldSelection;
        }
        return '(' + this.expression.getSourceString() + ")." + this.fieldSelection;
    }

    @Override
    public String toString() {
        return "GlslFieldNode{" +
                "expression=" + this.expression + ", " +
                "fieldSelection=" + this.fieldSelection + '}';
    }
}
