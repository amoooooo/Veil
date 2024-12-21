package foundry.veil.api.glsl.node.expression;

import com.google.common.collect.Streams;
import foundry.veil.api.glsl.node.GlslNode;

import java.util.stream.Stream;

/**
 * @author Ocelot
 */
public class GlslOperationNode implements GlslNode {

    private GlslNode first;
    private GlslNode second;
    private Operand operand;

    public GlslOperationNode(GlslNode first, GlslNode second, Operand operand) {
        this.first = first;
        this.second = second;
        this.operand = operand;
    }

    @Override
    public String getSourceString() {
        StringBuilder source = new StringBuilder();
        boolean firstOperation = this.first instanceof GlslOperationNode;
        boolean secondOperation = this.second instanceof GlslOperationNode;
        if (firstOperation) {
            source.append('(');
        }
        source.append(this.first.getSourceString());
        if (firstOperation) {
            source.append(')');
        }
        source.append(' ').append(this.operand.getDelimiter()).append(' ');
        if (secondOperation) {
            source.append('(');
        }
        source.append(this.second.getSourceString());
        if (secondOperation) {
            source.append(')');
        }
        return source.toString();
    }

    @Override
    public Stream<GlslNode> stream() {
        return Streams.concat(Stream.of(this), this.first.stream(), this.second.stream());
    }

    /**
     * @return The first operand
     */
    public GlslNode getFirst() {
        return this.first;
    }

    /**
     * @return The second operand
     */
    public GlslNode getSecond() {
        return this.second;
    }

    /**
     * @return The operand of relationship the expressions have
     */
    public Operand getOperand() {
        return this.operand;
    }

    public GlslOperationNode setFirst(GlslNode first) {
        this.first = first;
        return this;
    }

    public GlslOperationNode setSecond(GlslNode second) {
        this.second = second;
        return this;
    }

    public GlslOperationNode setOperand(Operand operand) {
        this.operand = operand;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslOperationNode that = (GlslOperationNode) o;
        return this.first.equals(that.first) && this.second.equals(that.second) && this.operand == that.operand;
    }

    @Override
    public int hashCode() {
        int result = this.first.hashCode();
        result = 31 * result + this.second.hashCode();
        result = 31 * result + this.operand.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "GlslOperationNode{first=" + this.first + ", second=" + this.second + ", operand=" + this.operand + '}';
    }

    public enum Operand {
        LEFT_SHIFT("<<"),
        RIGHT_SHIFT(">>"),
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        DIVIDE("/"),
        MODULO("%");

        private final String delimiter;

        Operand(String delimiter) {
            this.delimiter = delimiter;
        }

        public String getDelimiter() {
            return this.delimiter;
        }
    }
}
