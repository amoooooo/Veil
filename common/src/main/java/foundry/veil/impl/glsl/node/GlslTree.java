package foundry.veil.impl.glsl.node;

import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GlslTree {

    private GlslVersion version;
    private final List<GlslNode> body;

    public GlslTree(GlslVersion version, Collection<GlslNode> body) {
        this.version = version;
        this.body = new ArrayList<>(body);
    }

    public void visit(GlslVisitor visitor) {

    }

    public GlslVersion getVersion() {
        return this.version;
    }

    public List<GlslNode> getBody() {
        return this.body;
    }

    public void setVersion(GlslVersion version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "GlslTree{version=" + this.version + ", body=" + this.body + '}';
    }
}
