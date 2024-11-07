package foundry.veil.impl.glsl.node.function;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

public class GlslFunctionNode implements GlslNode {

    private GlslFunctionHeaderNode header;
    private GlslNode body;

    public GlslFunctionNode(GlslFunctionHeaderNode header, GlslNode body) {
        this.header = header;
        this.body = body;
    }

    @Override
    public void visit(GlslVisitor visitor) {

    }

    public GlslFunctionHeaderNode getHeader() {
        return this.header;
    }

    public GlslNode getBody() {
        return this.body;
    }

    public void setHeader(GlslFunctionHeaderNode header) {
        this.header = header;
    }

    public void setBody(GlslNode body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "GlslFunction{header=" + this.header + ", body=" + this.body + '}';
    }
}
