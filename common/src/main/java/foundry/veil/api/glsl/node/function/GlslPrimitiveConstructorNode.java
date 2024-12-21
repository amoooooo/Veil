package foundry.veil.api.glsl.node.function;

import foundry.veil.api.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.api.glsl.node.GlslNode;

import java.util.stream.Stream;

public class GlslPrimitiveConstructorNode implements GlslNode {

    private GlslTypeSpecifier primitiveType;

    public GlslPrimitiveConstructorNode(GlslTypeSpecifier primitiveType) {
        this.primitiveType = primitiveType;
    }

    public GlslTypeSpecifier getPrimitiveType() {
        return this.primitiveType;
    }

    public void setPrimitiveType(GlslTypeSpecifier primitiveType) {
        this.primitiveType = primitiveType;
    }

    @Override
    public String toString() {
        return "PrimitiveConstructorNode{operand=" + this.primitiveType + '}';
    }

    @Override
    public String getSourceString() {
        return this.primitiveType.getSourceString() + this.primitiveType.getPostSourceString();
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.of(this);
    }
}
