package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;

/**
 * Equality; A == B, A != B
 * <br>
 * Relational; A < B, A > B, A <= B, A >= B
 *
 * @author Ocelot
 */
public class GlslCompareNode implements GlslNode {

    private GlslNode first;
    private GlslNode second;
    private Operand operand;

    public GlslCompareNode(GlslNode first, GlslNode second, Operand operand) {
        this.first = first;
        this.second = second;
        this.operand = operand;
    }

    @Override
    public String getSourceString() {
        return this.first.getSourceString() + ' ' + this.operand.getDelimiter() + ' ' + this.second.getSourceString();
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

    public GlslCompareNode setFirst(GlslNode first) {
        this.first = first;
        return this;
    }

    public GlslCompareNode setSecond(GlslNode second) {
        this.second = second;
        return this;
    }

    public GlslCompareNode setOperand(Operand operand) {
        this.operand = operand;
        return this;
    }

    @Override
    public String toString() {
        return "GlslCompareNode{" +
                "first=" + this.first + ", " +
                "second=" + this.second + ", " +
                "operand=" + this.operand + '}';
    }

    public enum Operand {
        EQUAL("=="),
        NOT_EQUAL("!="),
        LESS("<"),
        GREATER(">"),
        LEQUAL("<="),
        GEQUAL(">=");

        private final String delimiter;

        Operand(String delimiter) {
            this.delimiter = delimiter;
        }

        public String getDelimiter() {
            return this.delimiter;
        }
    }
}
