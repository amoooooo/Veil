package foundry.veil.impl.glsl.grammar;

public class GlslStructField {

    private GlslSpecifiedType type;
    private String name;

    public GlslStructField(GlslType type, String name) {
        this.type = type.asSpecifiedType();
        this.name = name;
    }

    public GlslSpecifiedType getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public GlslStructField setType(GlslSpecifiedType type) {
        this.type = type;
        return this;
    }

    public GlslStructField setName(String name) {
        this.name = name;
        return this;
    }

    public String getSourceString() {
        return this.type.getSourceString() + " " + this.name + this.type.getPostSourceString();
    }
}
