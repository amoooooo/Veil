package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.visitor.GlslVisitor;

/**
 * Represents both while and do/while loops.
 *
 * @author Ocelot
 */
public class WhileLoopNode implements GlslNode {

    private GlslNode condition;
    private GlslNode body;
    private Type type;

    public WhileLoopNode(GlslNode condition, GlslNode body, Type type) {
        this.condition = condition;
        this.body = body;
        this.type = type;
    }

    @Override
    public void visit(GlslVisitor visitor) {
    }

    public GlslNode getCondition() {
        return this.condition;
    }

    public GlslNode getBody() {
        return this.body;
    }

    public Type getType() {
        return this.type;
    }

    public void setCondition(GlslNode condition) {
        this.condition = condition;
    }

    public void setBody(GlslNode body) {
        this.body = body;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {
        WHILE, DO
    }
}
