package foundry.veil.api.glsl.node.variable;

import com.google.common.collect.Streams;
import foundry.veil.api.glsl.node.GlslNode;

import java.util.stream.Stream;

public class GlslArrayNode implements GlslNode {

    private GlslNode expression;
    private GlslNode index;

    public GlslArrayNode(GlslNode expression, GlslNode index) {
        this.expression = expression;
        this.index = index;
    }

    public GlslNode getExpression() {
        return this.expression;
    }

    public GlslNode getIndex() {
        return this.index;
    }

    public GlslArrayNode setExpression(GlslNode expression) {
        this.expression = expression;
        return this;
    }

    public GlslArrayNode setIndex(GlslNode index) {
        this.index = index;
        return this;
    }

    @Override
    public String getSourceString() {
        return this.expression.getSourceString() + '[' + this.index.getSourceString() + ']';
    }

    @Override
    public Stream<GlslNode> stream() {
        return Streams.concat(Stream.of(this), this.expression.stream(), this.index.stream());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslArrayNode that = (GlslArrayNode) o;
        return this.expression.equals(that.expression) && this.index.equals(that.index);
    }

    @Override
    public int hashCode() {
        int result = this.expression.hashCode();
        result = 31 * result + this.index.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "GlslArrayNode{expression=" + this.expression + ", index=" + this.index + '}';
    }
}
