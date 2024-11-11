package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;

/**
 * Shift; A << B, A >> B
 * <br>
 * Add; A + B
 * <br>
 * Subtract; A - B
 * <br>
 * Multiply; A * B
 * <br>
 * Divide; A / B
 * <br>
 * Modulo; A % B
 *
 * @author Ocelot
 */
public final class GlslOperationNode implements GlslNode {

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
        return this.first.getSourceString() + " " + this.operand.getDelimiter() + " " + this.second.getSourceString();
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
