package foundry.veil.impl.glsl.visitor;

import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import org.jetbrains.annotations.Nullable;

public interface GlslTreeVisitor {

    void visitVersion(GlslVersion version);

    void visitField(GlslNewNode node);

    @Nullable GlslFunctionVisitor visitFunction(GlslFunctionNode node);

    void visitTreeEnd();
}
