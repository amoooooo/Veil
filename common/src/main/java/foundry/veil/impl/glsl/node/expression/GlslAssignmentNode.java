package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

/**
 * Equal; A = B
 * <br>
 * Multiply Assign; A *= B
 * <br>
 * Divide Assign; A /= B
 * <br>
 * Modulo Assign; A %= B
 * <br>
 * Add Assign; A += B
 * <br>
 * Subtract Assign; A -= B
 * <br>
 * Left Assign; A <<= B
 * <br>
 * Right Assign; A >>= B
 * <br>
 * And Assign; A &= B
 * <br>
 * Exclusive Or Assign; A ^= B
 * <br>
 * Or Assign; A |= B
 *
 * @param left    The left operand
 * @param right   The right operand
 * @param operand The operand to perform when setting the left to the right
 * @author Ocelot
 */
public record GlslAssignmentNode(GlslNode left, GlslNode right, Operand operand) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {
    }

    public enum Operand {
        EQUAL,
        MUL_ASSIGN,
        DIV_ASSIGN,
        MOD_ASSIGN,
        ADD_ASSIGN,
        SUB_ASSIGN,
        LEFT_ASSIGN,
        RIGHT_ASSIGN,
        AND_ASSIGN,
        XOR_ASSIGN,
        OR_ASSIGN
    }
}
