package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

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
 * @param first  The left operand
 * @param second The right operand
 * @param operand   The operand of relationship the expressions have
 * @author Ocelot
 */
public record GlslOperationNode(GlslNode first, GlslNode second, Operand operand) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {

    }

    public enum Operand {
        LEFT_SHIFT, RIGHT_SHIFT, ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO
    }
}
