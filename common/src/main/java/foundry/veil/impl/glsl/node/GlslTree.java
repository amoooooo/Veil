package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.grammar.GlslStructSpecifier;
import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslDeclaration;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.node.variable.GlslStructNode;
import foundry.veil.impl.glsl.visitor.GlslFunctionVisitor;
import foundry.veil.impl.glsl.visitor.GlslTreeVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class GlslTree {

    private final GlslVersion version;
    private final List<GlslNode> body;
    private final List<String> directives;

    public GlslTree(GlslVersion version, Collection<GlslNode> body, Collection<String> directives) {
        this.version = version;
        this.body = new ArrayList<>(body);
        this.directives = new ArrayList<>(directives);
    }

    private void visit(GlslTreeVisitor visitor, GlslNode node) {
        if (node instanceof GlslFunctionNode functionNode) {
            GlslFunctionVisitor functionVisitor = visitor.visitFunction(functionNode);
            if (functionVisitor != null) {
                functionNode.visit(functionVisitor);
            }
            return;
        }
        if (node instanceof GlslNewNode newNode) {
            visitor.visitField(newNode);
            return;
        }
        if (node instanceof GlslStructNode struct) {
            visitor.visitStruct(struct);
            return;
        }
        if (node instanceof GlslDeclaration declaration) {
            visitor.visitDeclaration(declaration);
            return;
        }
        throw new AssertionError("Not Possible");
    }

    public void visit(GlslTreeVisitor visitor) {
        visitor.visitVersion(this.version);
        for (String directive : this.directives) {
            visitor.visitDirective(directive);
        }

        for (GlslNode node : this.body) {
            if (node instanceof GlslEmptyNode) {
                continue;
            }
            if (node instanceof GlslCompoundNode compoundNode) {
                for (GlslNode child : compoundNode.getChildren()) {
                    this.visit(visitor, child);
                }
                continue;
            }
            this.visit(visitor, node);
        }

        visitor.visitTreeEnd();
    }

    public Stream<GlslFunctionNode> functions() {
        return this.body.stream().filter(node -> node instanceof GlslFunctionNode).map(node -> (GlslFunctionNode) node);
    }

    public Stream<GlslNewNode> fields() {
        return this.body.stream().filter(node -> node instanceof GlslNewNode).map(node -> (GlslNewNode) node);
    }

    public void add(GlslNode node) {
        this.body.add(node);
    }

    public GlslVersion getVersion() {
        return this.version;
    }

    public List<GlslNode> getBody() {
        return this.body;
    }

    public List<String> getDirectives() {
        return this.directives;
    }

    @Override
    public String toString() {
        return "GlslTree{version=" + this.version + ", body=" + this.body + '}';
    }
}
