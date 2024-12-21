package foundry.veil.api.glsl.node.branch;

import foundry.veil.api.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class GlslReturnNode implements GlslNode {

    private GlslNode value;

    public GlslReturnNode(@Nullable GlslNode value) {
        this.value = value;
    }

    public @Nullable GlslNode getValue() {
        return this.value;
    }

    public void setValue(@Nullable GlslNode value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslReturnNode that = (GlslReturnNode) o;
        return this.value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return "ReturnNode{value=" + this.value + '}';
    }

    @Override
    public String getSourceString() {
        return this.value != null ? "return " + this.value.getSourceString() : "return";
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.concat(Stream.of(this), this.value.stream());
    }
}
