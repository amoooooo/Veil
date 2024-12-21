package foundry.veil.api.glsl.visitor;

import foundry.veil.api.glsl.node.branch.GlslReturnNode;
import foundry.veil.api.glsl.node.expression.GlslAssignmentNode;
import foundry.veil.api.glsl.node.expression.GlslPrecisionNode;
import foundry.veil.api.glsl.node.function.GlslInvokeFunctionNode;
import foundry.veil.api.glsl.node.variable.GlslNewNode;

public interface GlslFunctionVisitor {

    void visitReturn(GlslReturnNode node);

    void visitAssignment(GlslAssignmentNode node);

    void visitPrecision(GlslPrecisionNode node);

    void visitInvokeFunction(GlslInvokeFunctionNode node);

    void visitNew(GlslNewNode node);

    void visitFunctionEnd();
}
