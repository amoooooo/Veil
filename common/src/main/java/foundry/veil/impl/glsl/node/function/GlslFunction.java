package foundry.veil.impl.glsl.node.function;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

public class GlslFunction implements GlslNode {

    private GlslFunctionHeader header;
    private GlslNode body;

    public GlslFunction(GlslFunctionHeader header, GlslNode body) {
        this.header = header;
        this.body = body;
    }

    @Override
    public void visit(GlslVisitor visitor) {

    }

    public GlslFunctionHeader getHeader() {
        return this.header;
    }

    public GlslNode getBody() {
        return this.body;
    }

    public void setHeader(GlslFunctionHeader header) {
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
