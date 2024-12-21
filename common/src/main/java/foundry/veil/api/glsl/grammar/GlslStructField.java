package foundry.veil.api.glsl.grammar;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslStructField that = (GlslStructField) o;
        return this.type.equals(that.type) && this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = this.type.hashCode();
        result = 31 * result + this.name.hashCode();
        return result;
    }

    public String getSourceString() {
        return this.type.getSourceString() + " " + this.name + this.type.getPostSourceString();
    }
}
