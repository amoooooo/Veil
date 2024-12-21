package foundry.veil.api.glsl.node.variable;

import foundry.veil.api.glsl.node.GlslNode;

import java.util.stream.Stream;

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
    public Stream<GlslNode> stream() {
        return Stream.concat(Stream.of(this), this.expression.stream());
    }

    @Override
    public String toString() {
        return "GlslFieldNode{expression=" + this.expression + ", fieldSelection=" + this.fieldSelection + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslFieldNode that = (GlslFieldNode) o;
        return this.expression.equals(that.expression) && this.fieldSelection.equals(that.fieldSelection);
    }

    @Override
    public int hashCode() {
        int result = this.expression.hashCode();
        result = 31 * result + this.fieldSelection.hashCode();
        return result;
    }
}
