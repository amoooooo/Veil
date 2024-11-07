package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;
import org.jetbrains.annotations.Nullable;

/**
 * if/else
 *
 * @param expression
 * @param first
 * @param branch
 * @author Ocelot
 */
public record GlslSelectionNode(GlslNode expression, GlslNode first, @Nullable GlslNode branch) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {

    }
}
