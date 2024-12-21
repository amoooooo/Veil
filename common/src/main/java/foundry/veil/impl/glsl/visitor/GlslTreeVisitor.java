package foundry.veil.impl.glsl.visitor;

import foundry.veil.impl.glsl.grammar.GlslVersionStatement;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslDeclaration;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.node.variable.GlslStructNode;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface GlslTreeVisitor {

    void visitMarkers(Map<String, GlslNode> markers);

    void visitVersion(GlslVersionStatement version);

    void visitDirective(String directive);

    void visitField(GlslNewNode newNode);

    void visitStruct(GlslStructNode structSpecifier);

    void visitDeclaration(GlslDeclaration declaration);

    @Nullable GlslFunctionVisitor visitFunction(GlslFunctionNode node);

    void visitTreeEnd();
}
