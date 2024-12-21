package foundry.veil.api.glsl.node.expression;

import com.google.common.collect.Streams;
import foundry.veil.api.glsl.node.GlslNode;

import java.util.stream.Stream;

/**
 * @author Ocelot
 */
public class GlslConditionalNode implements GlslNode {

    private GlslNode condition;
    private GlslNode first;
    private GlslNode second;

    public GlslConditionalNode(GlslNode condition, GlslNode first, GlslNode second) {
        this.condition = condition;
        this.first = first;
        this.second = second;
    }

    public GlslNode getCondition() {
        return this.condition;
    }

    public GlslNode getFirst() {
        return this.first;
    }

    public GlslNode getSecond() {
        return this.second;
    }

    public GlslConditionalNode setCondition(GlslNode condition) {
        this.condition = condition;
        return this;
    }

    public GlslConditionalNode setFirst(GlslNode first) {
        this.first = first;
        return this;
    }

    public GlslConditionalNode setSecond(GlslNode second) {
        this.second = second;
        return this;
    }

    @Override
    public String getSourceString() {
        return '(' + this.condition.getSourceString() + " ? " + this.first.getSourceString() + " : " + this.second.getSourceString() + ')';
    }

    @Override
    public Stream<GlslNode> stream() {
        return Streams.concat(Stream.of(this), this.first.stream(), this.second.stream());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslConditionalNode that = (GlslConditionalNode) o;
        return this.condition.equals(that.condition) && this.first.equals(that.first) && this.second.equals(that.second);
    }

    @Override
    public int hashCode() {
        int result = this.condition.hashCode();
        result = 31 * result + this.first.hashCode();
        result = 31 * result + this.second.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "GlslConditionalNode{" +
                "condition=" + this.condition + ", " +
                "first=" + this.first + ", " +
                "second=" + this.second + '}';
    }
}
