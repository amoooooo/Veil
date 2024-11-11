package foundry.veil.impl.glsl.node.primary;

import foundry.veil.impl.glsl.node.GlslConstantNode;

public record GlslIntConstantNode(GlslIntFormat format, boolean signed, int value) implements GlslConstantNode {

    @Override
    public Number numberValue() {
        return this.value;
    }

    @Override
    public int intValue() {
        return this.value;
    }

    @Override
    public boolean booleanValue() {
        return this.value != 0.0;
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public String getSourceString() {
        return switch (this.format) {
            case HEXADECIMAL -> "0x" + Integer.toHexString(this.value);
            case OCTAL -> Integer.toOctalString(this.value);
            case DECIMAL -> Integer.toString(this.value);
        };
    }

    @Override
    public String toString() {
        return this.getSourceString();
    }
}
