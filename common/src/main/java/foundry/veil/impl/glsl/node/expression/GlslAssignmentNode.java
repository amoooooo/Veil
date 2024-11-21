package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;

/**
 * @author Ocelot
 */
public class GlslAssignmentNode implements GlslNode {

    private GlslNode first;
    private GlslNode second;
    private Operand operand;

    /**
     * @param first   The first operand
     * @param second  The second operand
     * @param operand The operand to perform when setting the first to the second
     */
    public GlslAssignmentNode(GlslNode first, GlslNode second, Operand operand) {
        this.first = first;
        this.second = second;
        this.operand = operand;
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
     * @return The operand to perform when setting the first to the second
     */
    public Operand getOperand() {
        return this.operand;
    }

    public GlslAssignmentNode setFirst(GlslNode first) {
        this.first = first;
        return this;
    }

    public GlslAssignmentNode setSecond(GlslNode second) {
        this.second = second;
        return this;
    }

    public GlslAssignmentNode setOperand(Operand operand) {
        this.operand = operand;
        return this;
    }

    @Override
    public String getSourceString() {
        return this.first.getSourceString() + ' ' + this.operand.getDelimiter() + ' ' + this.second.getSourceString();
    }

    @Override
    public String toString() {
        return "GlslAssignmentNode{" +
                "first=" + this.first + ", " +
                "second=" + this.second + ", " +
                "operand=" + this.operand + ']';
    }

    public enum Operand {
        EQUAL("="),
        MUL_ASSIGN("*="),
        DIV_ASSIGN("/="),
        MOD_ASSIGN("%="),
        ADD_ASSIGN("+="),
        SUB_ASSIGN("-="),
        LEFT_ASSIGN("<<="),
        RIGHT_ASSIGN(">>="),
        AND_ASSIGN("&="),
        XOR_ASSIGN("^="),
        OR_ASSIGN("|=");

        private final String delimiter;

        Operand(String delimiter) {
            this.delimiter = delimiter;
        }

        public String getDelimiter() {
            return this.delimiter;
        }
    }
}
