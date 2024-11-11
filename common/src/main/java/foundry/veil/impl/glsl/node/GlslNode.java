package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.visitor.GlslNodeVisitor;

import java.util.*;

public interface GlslNode {

    String getSourceString();

    default void visit(GlslNodeVisitor visitor) {
        visitor.visitNode(this);
    }

    /**
     * @return A new list with the child contents of this node
     */
    default List<GlslNode> toList() {
        return new ArrayList<>(Collections.singleton(this));
    }

    static void visitAll(Collection<GlslNode> nodes, GlslNodeVisitor visitor) {
        for (GlslNode node : nodes) {
            node.visit(visitor);
        }
    }

    static GlslNode compound(Collection<GlslNode> nodes) {
        if (nodes.isEmpty()) {
            return GlslEmptyNode.INSTANCE;
        }
        if (nodes.size() == 1) {
            return nodes.iterator().next();
        }
        List<GlslNode> list = new ArrayList<>();
        for (GlslNode node : nodes) {
            if (!(node instanceof GlslCompoundNode compoundNode)) {
                list.clear();
                list.addAll(nodes);
                break;
            }
            list.addAll(compoundNode.getChildren());
        }
        return new GlslCompoundNode(list);
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
