package foundry.veil.impl.glsl.visitor;

import foundry.veil.impl.glsl.node.branch.ForLoopNode;
import foundry.veil.impl.glsl.node.branch.GlslCaseLabelNode;
import foundry.veil.impl.glsl.node.variable.GlslFieldNode;

public interface GlslVisitor {

    default GlslFieldVisitor visitField(GlslFieldNode node) {
        return null;
    }

    default void visitForLoop(ForLoopNode node) {
    }

    default void visitCaseLabel(GlslCaseLabelNode node) {
    }
}
