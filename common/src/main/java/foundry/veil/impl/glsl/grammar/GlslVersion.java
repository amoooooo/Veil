package foundry.veil.impl.glsl.grammar;

public record GlslVersion(int version, boolean core) {

    public GlslVersion withVersion(int version) {
        return new GlslVersion(version, this.core);
    }

    public GlslVersion withCore(boolean core) {
        return new GlslVersion(this.version, core);
    }
}
