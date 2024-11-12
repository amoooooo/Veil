package foundry.veil.impl.glsl.node.function;

import foundry.veil.impl.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.impl.glsl.node.GlslNode;

public class GlslPrimitiveConstructorNode implements GlslNode {

    private GlslTypeSpecifier type;

    public GlslPrimitiveConstructorNode(GlslTypeSpecifier type) {
        this.type = type;
    }

    public GlslTypeSpecifier getType() {
        return this.type;
    }

    public void setType(GlslTypeSpecifier type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "PrimitiveConstructorNode{operand=" + this.type + '}';
    }

    @Override
    public String getSourceString() {
        return this.type.getSourceString() + this.type.getPostSourceString();
    }
}
