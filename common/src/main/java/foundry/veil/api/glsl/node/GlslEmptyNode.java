package foundry.veil.api.glsl.node;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public enum GlslEmptyNode implements GlslNode {
    INSTANCE;

    @Override
    public String getSourceString() {
        return "";
    }

    @Override
    public List<GlslNode> toList() {
        return new ArrayList<>();
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.empty();
    }

}
