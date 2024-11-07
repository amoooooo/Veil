package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

/**
 * Equality; A == B, A != B
 * <br>
 * Relational; A < B, A > B, A <= B, A >= B
 *
 * @param first  The left operand
 * @param second The right operand
 * @param type   The operand of relationship the expressions have
 * @author Ocelot
 */
public record GlslCompareNode(GlslNode first, GlslNode second, Type type) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {

    }

    public enum Type {
        EQUAL, NOT_EQUAL, LESS, GREATER, LEQUAL, GEQUAL
    }
}
