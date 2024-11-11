package foundry.veil.impl.glsl.visitor;

import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import org.jetbrains.annotations.Nullable;

public class GlslStringWriter implements GlslTreeVisitor, GlslFieldVisitor {

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
    public void visitField(GlslNewNode node) {
        this.builder.append(this.formatExpression(node)).append(";\n");
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
