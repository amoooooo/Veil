package foundry.veil.api.glsl.node.expression;

import foundry.veil.api.glsl.grammar.GlslSpecifiedType;
import foundry.veil.api.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class GlslUnaryNode implements GlslNode {

    private GlslNode expression;
    private Operand operand;

    public GlslUnaryNode(GlslNode expression, Operand operand) {
        this.expression = expression;
        this.operand = operand;
    }

    @Override
    public @Nullable GlslSpecifiedType getType() {
        return this.expression.getType();
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.concat(Stream.of(this), this.expression.stream());
    }

    public GlslNode getExpression() {
        return this.expression;
    }

    public Operand getOperand() {
        return this.operand;
    }

    public GlslUnaryNode setExpression(GlslNode expression) {
        this.expression = expression;
        return this;
    }

    public GlslUnaryNode setOperand(Operand operand) {
        this.operand = operand;
        return this;
    }

    @Override
    public String toString() {
        return "GlslUnaryNode{expression=" + this.expression + ", " + "operand=" + this.operand + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslUnaryNode that = (GlslUnaryNode) o;
        return this.expression.equals(that.expression) && this.operand == that.operand;
    }

    @Override
    public int hashCode() {
        int result = this.expression.hashCode();
        result = 31 * result + this.operand.hashCode();
        return result;
    }

    @Override
    public String getSourceString() {
        return switch (this.operand) {
            case PRE_INCREMENT,
                 PRE_DECREMENT,
                 PLUS,
                 DASH,
                 BANG,
                 TILDE -> this.operand.getDelimiter() + '(' + this.expression.getSourceString() + ')';
            case POST_INCREMENT, POST_DECREMENT -> this.expression.getSourceString() + this.operand.getDelimiter();
        };
    }

    public enum Operand {
        PRE_INCREMENT("++"),
        PRE_DECREMENT("--"),
        POST_INCREMENT("++"),
        POST_DECREMENT("--"),
        PLUS("+"),
        DASH("-"),
        BANG("!"),
        TILDE("~");

        private final String delimiter;

        Operand(String delimiter) {
            this.delimiter = delimiter;
        }

        public String getDelimiter() {
            return this.delimiter;
        }
    }
}
