package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
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
    public String getSourceString() {
        StringBuilder builder = new StringBuilder("if (");
        builder.append(this.expression.getSourceString().replaceAll("\n", "\n\t")).append(") {\n");
        builder.append(this.first.getSourceString()).append("\n}");
        if (this.branch != null) {
            builder.append(" else {\n");
            builder.append(this.branch.getSourceString().replaceAll("\n", "\n\t")).append("\n}");
        }
        return builder.toString();
    }
}
