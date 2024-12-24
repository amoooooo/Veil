package foundry.veil.api.glsl.visitor;

import foundry.veil.api.glsl.grammar.GlslVersionStatement;
import foundry.veil.api.glsl.node.GlslNode;
import foundry.veil.api.glsl.node.function.GlslFunctionNode;
import foundry.veil.api.glsl.node.variable.GlslDeclarationNode;
import foundry.veil.api.glsl.node.variable.GlslNewNode;
import foundry.veil.api.glsl.node.variable.GlslStructNode;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface GlslTreeVisitor {

    void visitMarkers(Map<String, GlslNode> markers);

    void visitVersion(GlslVersionStatement version);

    void visitDirective(String directive);

    void visitMacro(String key, String value);

    void visitField(GlslNewNode newNode);

    void visitStruct(GlslStructNode structSpecifier);

    void visitDeclaration(GlslDeclarationNode declaration);

    @Nullable GlslFunctionVisitor visitFunction(GlslFunctionNode node);

    void visitTreeEnd();
}
