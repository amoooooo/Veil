package foundry.veil.impl.glsl.visitor;

import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslDeclaration;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.node.variable.GlslStructNode;
import org.jetbrains.annotations.Nullable;

public interface GlslTreeVisitor {

    void visitVersion(GlslVersion version);

    void visitDirective(String directive);

    void visitField(GlslNewNode newNode);

    void visitStruct(GlslStructNode structSpecifier);

    void visitDeclaration(GlslDeclaration declaration);

    @Nullable GlslFunctionVisitor visitFunction(GlslFunctionNode node);

    void visitTreeEnd();
}
