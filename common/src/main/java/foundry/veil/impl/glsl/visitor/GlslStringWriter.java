package foundry.veil.impl.glsl.visitor;

import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslDeclaration;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.node.variable.GlslStructNode;
import org.jetbrains.annotations.Nullable;

public class GlslStringWriter implements GlslTreeVisitor {

    private final StringBuilder builder;
    private String value;

    public GlslStringWriter() {
        this.builder = new StringBuilder();
        this.value = "";
    }

    private String formatExpression(GlslNode node) {
        return node.getSourceString();
    }

    @Override
    public void visitVersion(GlslVersion version) {
        this.builder.append("#version ").append(version.getVersionStatement()).append("\n\n");
    }

    @Override
    public void visitDirective(String directive) {
        this.builder.append(directive).append('\n');
    }

    @Override
    public void visitField(GlslNewNode newNode) {
        this.builder.append(this.formatExpression(newNode)).append(";\n");
    }

    @Override
    public void visitStruct(GlslStructNode structSpecifier) {
        this.builder.append(this.formatExpression(structSpecifier)).append(";\n");
    }

    @Override
    public void visitDeclaration(GlslDeclaration declaration) {
        this.builder.append(this.formatExpression(declaration)).append(";\n");
    }

    @Override
    public @Nullable GlslFunctionVisitor visitFunction(GlslFunctionNode node) {
        this.builder.append(node.getSourceString());
        return null;
    }

    @Override
    public void visitTreeEnd() {
        this.value = this.builder.toString();
        this.builder.setLength(0);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
