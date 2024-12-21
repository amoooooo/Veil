package foundry.veil.api.glsl.visitor;

import foundry.veil.api.glsl.node.GlslNode;

@FunctionalInterface
public interface GlslNodeVisitor {

    void visitNode(GlslNode node);
}
