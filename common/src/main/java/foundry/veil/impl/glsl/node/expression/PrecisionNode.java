package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.grammar.GlslTypeQualifier;
import foundry.veil.impl.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

public class PrecisionNode implements GlslNode {

    private GlslTypeQualifier.Precision precision;
    private GlslTypeSpecifier typeSpecifier;

    public PrecisionNode(GlslTypeQualifier.Precision precision, GlslTypeSpecifier typeSpecifier) {
        this.precision = precision;
        this.typeSpecifier = typeSpecifier;
    }

    @Override
    public void visit(GlslVisitor visitor) {

    }

    public GlslTypeQualifier.Precision getPrecision() {
        return this.precision;
    }

    public GlslTypeSpecifier getTypeSpecifier() {
        return this.typeSpecifier;
    }

    public void setPrecision(GlslTypeQualifier.Precision precision) {
        this.precision = precision;
    }

    public void setTypeSpecifier(GlslTypeSpecifier typeSpecifier) {
        this.typeSpecifier = typeSpecifier;
    }
}
