package foundry.veil.impl.glsl.grammar;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class GlslParameterDeclaration {

    private String name;
    private GlslSpecifiedType type;

    public GlslParameterDeclaration(@Nullable String name, GlslSpecifiedType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public GlslSpecifiedType getType() {
        return this.type;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public void setType(GlslSpecifiedType type) {
        this.type = type;
    }

    public GlslParameterDeclaration setQualifiers(Collection<GlslTypeQualifier> qualifiers) {
        this.type.setQualifiers(qualifiers);
        return this;
    }
}
