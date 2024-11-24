package foundry.veil.impl.glsl.node.branch;

import com.google.common.collect.Streams;
import foundry.veil.impl.glsl.node.GlslNode;

import java.util.stream.Stream;

/**
 * Represents both while and do/while loops.
 *
 * @author Ocelot
 */
public class WhileLoopNode implements GlslNode {

    private GlslNode condition;
    private GlslNode body;
    private Type loopType;

    public WhileLoopNode(GlslNode condition, GlslNode body, Type loopType) {
        this.condition = condition;
        this.body = body;
        this.loopType = loopType;
    }

    public GlslNode getCondition() {
        return this.condition;
    }

    public GlslNode getBody() {
        return this.body;
    }

    public Type getLoopType() {
        return this.loopType;
    }

    public WhileLoopNode setCondition(GlslNode condition) {
        this.condition = condition;
        return this;
    }

    public WhileLoopNode setBody(GlslNode body) {
        this.body = body;
        return this;
    }

    public WhileLoopNode setLoopType(Type loopType) {
        this.loopType = loopType;
        return this;
    }

    @Override
    public String getSourceString() {
        return "while (" + this.condition.getSourceString() + ") {\n" + this.body.getSourceString().replaceAll("\n", "\n\t") + "\n}";
    }

    @Override
    public Stream<GlslNode> stream() {
        return Streams.concat(Stream.of(this), this.condition.stream(), this.body.stream());
    }

    public enum Type {
        WHILE, DO
    }
}
