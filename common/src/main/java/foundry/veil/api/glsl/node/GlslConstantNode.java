package foundry.veil.api.glsl.node;

import java.util.stream.Stream;

public interface GlslConstantNode extends GlslNode {

    Number numberValue();

    default double doubleValue() {
        return this.numberValue().doubleValue();
    }

    default float floatValue() {
        return this.numberValue().floatValue();
    }

    default int intValue() {
        return this.numberValue().intValue();
    }

    default long unsignedIntValue() {
        return Integer.toUnsignedLong(this.numberValue().intValue());
    }

    boolean booleanValue();

    boolean isNumber();

    @Override
    default Stream<GlslNode> stream() {
        return Stream.of(this);
    }
}
