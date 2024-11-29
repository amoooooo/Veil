package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.grammar.GlslSpecifiedType;
import foundry.veil.impl.glsl.node.primary.GlslBoolConstantNode;
import foundry.veil.impl.glsl.node.primary.GlslFloatConstantNode;
import foundry.veil.impl.glsl.node.primary.GlslIntConstantNode;
import foundry.veil.impl.glsl.node.primary.GlslIntFormat;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public interface GlslNode {

    String getSourceString();

    /**
     * @return The type of this node if it is a field
     */
    default @Nullable GlslSpecifiedType getType() {
        return null;
    }

    /**
     * @return A new list with the child contents of this node
     */
    default List<GlslNode> toList() {
        return new ArrayList<>(Collections.singleton(this));
    }

    /**
     * @return The body of this node or <code>null</code> if there is no sub-body in this node
     */
    default @Nullable List<GlslNode> getBody() {
        return null;
    }

    /**
     * Sets the body of this node.
     *
     * @param body The new body
     * @return Whether the action was successful
     */
    default boolean setBody(Collection<GlslNode> body) {
        List<GlslNode> nodes = this.getBody();
        if (nodes != null) {
            nodes.clear();
            nodes.addAll(body);
            return true;
        }
        return false;
    }

    /**
     * Sets the body of this node.
     *
     * @param body The new body
     * @return Whether the action was successful
     */
    default boolean setBody(GlslNode... body) {
        return this.setBody(Arrays.asList(body));
    }

    Stream<GlslNode> stream();

    static GlslIntConstantNode intConstant(int value) {
        return new GlslIntConstantNode(GlslIntFormat.DECIMAL, true, value);
    }

    static GlslIntConstantNode unsignedIntConstant(int value) {
        return new GlslIntConstantNode(GlslIntFormat.DECIMAL, false, value);
    }

    static GlslFloatConstantNode floatConstant(float value) {
        return new GlslFloatConstantNode(value);
    }

    static GlslBoolConstantNode booleanConstant(boolean value) {
        return new GlslBoolConstantNode(value);
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
