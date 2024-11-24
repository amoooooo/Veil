package foundry.veil.impl.glsl.node.branch;

import foundry.veil.impl.glsl.node.GlslNode;

import java.util.Locale;
import java.util.stream.Stream;

public enum JumpNode implements GlslNode {
    CONTINUE, BREAK, DISCARD;

    @Override
    public String getSourceString() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.of(this);
    }
}
