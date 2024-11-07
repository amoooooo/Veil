package foundry.veil.impl.glsl.node.function;

import foundry.veil.impl.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

public class PrimitiveConstructorNode implements GlslNode {

    private GlslTypeSpecifier type;

    public PrimitiveConstructorNode(GlslTypeSpecifier type) {
        this.type = type;
    }

    public GlslTypeSpecifier getType() {
        return this.type;
    }

    public void setType(GlslTypeSpecifier type) {
        this.type = type;
    }

    @Override
    public void visit(GlslVisitor visitor) {

    }

    @Override
    public String toString() {
        return "PrimitiveConstructorNode{type=" + this.type + '}';
    }
}
