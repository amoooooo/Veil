package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public interface GlslNode {

    void visit(GlslVisitor visitor);

    default Collection<GlslNode> children() {
        return Collections.emptySet();
    }

    static GlslNode compound(Collection<GlslNode> nodes) {
        if (nodes.isEmpty()) {
            return GlslEmptyNode.INSTANCE;
        }
        if (nodes.size() == 1) {
            return nodes.iterator().next();
        }
        return new GlslCompoundNode(new ArrayList<>(nodes));
    }

    static GlslNode compound(GlslNode... nodes) {
        if (nodes.length == 0) {
            return GlslEmptyNode.INSTANCE;
        }
        if (nodes.length == 1) {
            return nodes[0];
        }
        return new GlslCompoundNode(new ArrayList<>(Arrays.asList(nodes)));
    }
}
