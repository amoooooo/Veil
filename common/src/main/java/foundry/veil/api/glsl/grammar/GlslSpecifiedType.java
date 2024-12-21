package foundry.veil.api.glsl.grammar;

import foundry.veil.api.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Specifies the full operand of something in GLSL in addition to all qualifiers.
 */
public class GlslSpecifiedType implements GlslType {

    private GlslTypeSpecifier specifier;
    private final List<GlslTypeQualifier> qualifiers;

    public GlslSpecifiedType(GlslTypeSpecifier specifier) {
        this.specifier = specifier;
        this.qualifiers = new ArrayList<>();
    }

    public GlslSpecifiedType(GlslTypeSpecifier specifier, Collection<GlslTypeQualifier> qualifiers) {
        this(specifier);
        this.qualifiers.addAll(qualifiers);
    }

    public GlslSpecifiedType(GlslTypeSpecifier specifier, GlslTypeQualifier... qualifiers) {
        this(specifier);
        this.qualifiers.addAll(Arrays.asList(qualifiers));
    }

    /**
     * @return The operand of the field, method, etc
     */
    public GlslTypeSpecifier getSpecifier() {
        return this.specifier;
    }

    /**
     * Adds a layout id to the qualifier list, or adds to an existing layout.
     *
     * @param identifier The name of the identifier
     * @param expression The value to assign it to
     */
    public GlslSpecifiedType addLayoutId(String identifier, @Nullable GlslNode expression) {
        for (GlslTypeQualifier qualifier : this.qualifiers) {
            if (qualifier instanceof GlslTypeQualifier.Layout(List<GlslTypeQualifier.LayoutId> layoutIds)) {
                layoutIds.add(new GlslTypeQualifier.LayoutId(identifier, expression));
                return this;
            }
        }

        this.qualifiers.addFirst(GlslTypeQualifier.layout(Collections.singleton(new GlslTypeQualifier.LayoutId(identifier, expression))));
        return this;
    }

    /**
     * @return The qualifiers applied to it, for example <code>layout()</code> or <code>flat</code>
     */
    public List<GlslTypeQualifier> getQualifiers() {
        return this.qualifiers;
    }

    /**
     * Sets the operand of this
     *
     * @param specifier
     * @return
     */
    public GlslSpecifiedType setSpecifier(GlslTypeSpecifier specifier) {
        this.specifier = specifier;
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

    public String getSourceString() {
        StringBuilder builder = new StringBuilder();
        for (GlslTypeQualifier qualifier : this.qualifiers) {
            builder.append(qualifier.getSourceString()).append(" ");
        }
        builder.append(this.specifier.getSourceString());
        return builder.toString();
    }

    public String getPostSourceString() {
        return this.specifier.getPostSourceString();
    }

    @Override
    public String toString() {
        return "GlslSpecifiedType[specifier=" + this.specifier + ", qualifiers=" + this.qualifiers;
    }

    @Override
    public GlslSpecifiedType asSpecifiedType() {
        return this;
    }
}
