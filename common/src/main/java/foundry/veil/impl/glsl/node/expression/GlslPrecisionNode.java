package foundry.veil.impl.glsl.node.expression;

import foundry.veil.impl.glsl.grammar.GlslTypeQualifier;
import foundry.veil.impl.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.impl.glsl.node.GlslNode;

public class GlslPrecisionNode implements GlslNode {

    private GlslTypeQualifier.Precision precision;
    private GlslTypeSpecifier typeSpecifier;

    public GlslPrecisionNode(GlslTypeQualifier.Precision precision, GlslTypeSpecifier typeSpecifier) {
        this.precision = precision;
        this.typeSpecifier = typeSpecifier;
    }

    public GlslTypeQualifier.Precision getPrecision() {
        return this.precision;
    }

    public GlslTypeSpecifier getTypeSpecifier() {
        return this.typeSpecifier;
    }

    public GlslPrecisionNode setPrecision(GlslTypeQualifier.Precision precision) {
        this.precision = precision;
        return this;
    }

    public GlslPrecisionNode setTypeSpecifier(GlslTypeSpecifier typeSpecifier) {
        this.typeSpecifier = typeSpecifier;
        return this;
    }

    @Override
    public String getSourceString() {
        return "precision " + this.precision.getSourceString() + " " + this.typeSpecifier.getSourceString();
    }
}
