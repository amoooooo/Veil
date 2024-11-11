package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.visitor.GlslFunctionVisitor;
import foundry.veil.impl.glsl.visitor.GlslTreeVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GlslTree {

    private final GlslVersion version;
    private final List<GlslNode> body;

    public GlslTree(GlslVersion version, Collection<GlslNode> body) {
        this.version = version;
        this.body = new ArrayList<>(body);
    }

    public void visit(GlslTreeVisitor visitor) {
        visitor.visitVersion(this.version);
        GlslNode.visitAll(this.body, node -> {
            if (node instanceof GlslFunctionNode functionNode) {
                GlslFunctionVisitor functionVisitor = visitor.visitFunction(functionNode);
                if (functionVisitor != null) {
                    functionNode.visit(functionVisitor);
                }
                return;
            }
            if(node instanceof GlslNewNode newNode) {
                visitor.visitField(newNode);
                return;
            }
            System.out.println(node);
        });
        visitor.visitTreeEnd();
    }

    public GlslVersion getVersion() {
        return this.version;
    }

    public List<GlslNode> getBody() {
        return this.body;
    }

    @Override
    public String toString() {
        return "GlslTree{version=" + this.version + ", body=" + this.body + '}';
    }
}
