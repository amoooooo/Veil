package foundry.veil.impl.glsl.visitor;

import foundry.veil.impl.glsl.node.GlslNode;

@FunctionalInterface
public interface GlslNodeVisitor {

    void visitNode(GlslNode node);
}
