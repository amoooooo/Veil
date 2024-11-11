package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;

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

    public GlslNode getCondition() {
        return this.condition;
    }

    public GlslNode getBody() {
        return this.body;
    }

    public Type getType() {
        return this.type;
    }

    public WhileLoopNode setCondition(GlslNode condition) {
        this.condition = condition;
        return this;
    }

    public WhileLoopNode setBody(GlslNode body) {
        this.body = body;
        return this;
    }

    public WhileLoopNode setType(Type type) {
        this.type = type;
        return this;
    }

    @Override
    public String getSourceString() {
        return "while (" + this.condition.getSourceString() + ") {\n" + this.body.getSourceString().replaceAll("\n", "\n\t") + "\n}";
    }

    public enum Type {
        WHILE, DO
    }
}
