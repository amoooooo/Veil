package foundry.veil.impl.glsl.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Specifies the full operand of something in GLSL in addition to all qualifiers.
 */
public class GlslSpecifiedType implements GlslType {

    private GlslTypeSpecifier type;
    private final List<GlslTypeQualifier> qualifiers;

    public GlslSpecifiedType(GlslTypeSpecifier type) {
        this.type = type;
        this.qualifiers = new ArrayList<>();
    }

    public GlslSpecifiedType(GlslTypeSpecifier type, Collection<GlslTypeQualifier> qualifiers) {
        this(type);
        this.qualifiers.addAll(qualifiers);
    }

    public GlslSpecifiedType(GlslTypeSpecifier type, GlslTypeQualifier... qualifiers) {
        this(type);
        this.qualifiers.addAll(Arrays.asList(qualifiers));
    }

    /**
     * @return The operand of the field, method, etc
     */
    public GlslTypeSpecifier getType() {
        return this.type;
    }

    /**
     * @return The qualifiers applied to it, for example <code>layout()</code> or <code>flat</code>
     */
    public List<GlslTypeQualifier> getQualifiers() {
        return this.qualifiers;
    }

    /**
     * Sets the type of this
     * @param type
     * @return
     */
    public GlslSpecifiedType setType(GlslTypeSpecifier type) {
        this.type = type;
        return this;
    }

    public GlslSpecifiedType setQualifiers(GlslTypeQualifier... qualifiers) {
        this.qualifiers.clear();
        this.qualifiers.addAll(Arrays.asList(qualifiers));
        return this;
    }

    public GlslSpecifiedType setQualifiers(Collection<GlslTypeQualifier> qualifiers) {
        this.qualifiers.clear();
        this.qualifiers.addAll(qualifiers);
        return this;
    }

    @Override
    public String toString() {
        return "GlslSpecifiedType[specifier=" + this.type + ", qualifiers=" + this.qualifiers;
    }

    @Override
    public GlslSpecifiedType asSpecifiedType() {
        return this;
    }
}
