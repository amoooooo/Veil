package foundry.veil.impl.glsl.visitor;

import foundry.veil.impl.glsl.node.branch.GlslReturnNode;
import foundry.veil.impl.glsl.node.expression.GlslAssignmentNode;
import foundry.veil.impl.glsl.node.expression.GlslPrecisionNode;
import foundry.veil.impl.glsl.node.function.GlslInvokeFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;

public interface GlslFunctionVisitor {

    void visitReturn(GlslReturnNode node);

    void visitAssignment(GlslAssignmentNode node);

    void visitPrecision(GlslPrecisionNode node);

    void visitInvokeFunction(GlslInvokeFunctionNode node);

    void visitNew(GlslNewNode node);

    void visitFunctionEnd();
}
