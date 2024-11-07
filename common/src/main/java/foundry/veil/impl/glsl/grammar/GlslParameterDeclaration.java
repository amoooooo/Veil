package foundry.veil.impl.glsl.grammar;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Represents a single parameter declaration. Includes the name and full data type of the parameter.
 *
 * @author Ocelot
 */
public class GlslParameterDeclaration {

    private String name;
    private GlslSpecifiedType type;

    public GlslParameterDeclaration(@Nullable String name, GlslType type) {
        this.name = name;
        this.type = type.asSpecifiedType();
    }

    /**
     * @return The name of the parameter or <code>null</code> if declared like <code>void foo(int)</code>
     */
    public @Nullable String getName() {
        return this.name;
    }

    /**
     * @return The parameter data type
     */
    public GlslSpecifiedType getType() {
        return this.type;
    }

    /**
     * Sets the name of this parameter.
     *
     * @param name The new name
     */
    public GlslParameterDeclaration setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the data type of this parameter.
     *
     * @param type The new type
     */
    public GlslParameterDeclaration setType(GlslType type) {
        this.type = type.asSpecifiedType();
        return this;
    }

    /**
     * Sets The qualifiers of this parameter type.
     *
     * @param qualifiers The new qualifiers
     */
    public GlslParameterDeclaration setQualifiers(GlslTypeQualifier... qualifiers) {
        this.type.setQualifiers(qualifiers);
        return this;
    }

    /**
     * Sets The qualifiers of this parameter type.
     *
     * @param qualifiers The new qualifiers
     */
    public GlslParameterDeclaration setQualifiers(Collection<GlslTypeQualifier> qualifiers) {
        this.type.setQualifiers(qualifiers);
        return this;
    }

    @Override
    public String toString() {
        return "GlslParameterDeclaration{name='" + this.name + "', type=" + this.type + '}';
    }
}
