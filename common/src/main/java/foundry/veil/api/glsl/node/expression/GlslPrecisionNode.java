package foundry.veil.api.glsl.node.expression;

import foundry.veil.api.glsl.grammar.GlslTypeQualifier;
import foundry.veil.api.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.api.glsl.node.GlslNode;

import java.util.stream.Stream;

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

    @Override
    public Stream<GlslNode> stream() {
        return Stream.of(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslPrecisionNode that = (GlslPrecisionNode) o;
        return this.precision == that.precision && this.typeSpecifier.equals(that.typeSpecifier);
    }

    @Override
    public int hashCode() {
        int result = this.precision.hashCode();
        result = 31 * result + this.typeSpecifier.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "GlslPrecisionNode{precision=" + this.precision + ", typeSpecifier=" + this.typeSpecifier + '}';
    }
}
