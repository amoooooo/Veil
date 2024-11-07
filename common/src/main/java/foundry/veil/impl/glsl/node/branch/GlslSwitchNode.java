package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.List;

/**
 * Switch statement.
 *
 * @param condition The condition inside the switch <code>switch(condition) {}</code>
 * @param branches  All code inside the switch, including all labels and code under those labels
 * @author Ocelot
 */
public record GlslSwitchNode(GlslNode condition, List<GlslNode> branches) implements GlslNode {

    @Override
    public void visit(GlslVisitor visitor) {

    }
}
