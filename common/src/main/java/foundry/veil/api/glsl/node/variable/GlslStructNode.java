package foundry.veil.api.glsl.node.variable;

import foundry.veil.api.glsl.grammar.GlslSpecifiedType;
import foundry.veil.api.glsl.grammar.GlslStructSpecifier;
import foundry.veil.api.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.api.glsl.node.GlslNode;
import foundry.veil.api.glsl.node.GlslRootNode;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public class GlslStructNode implements GlslRootNode {

    private GlslSpecifiedType specifiedType;

    public GlslStructNode(GlslSpecifiedType specifiedType) {
        if (!specifiedType.getSpecifier().isStruct()) {
            throw new IllegalArgumentException("specified type must be struct or array of structs");
        }
        this.specifiedType = specifiedType;
    }

    public GlslSpecifiedType getSpecifiedType() {
        return this.specifiedType;
    }

    public GlslStructSpecifier getStructSpecifier() {
        GlslTypeSpecifier specifier = this.specifiedType.getSpecifier();
        while (specifier instanceof GlslTypeSpecifier.Array array) {
            specifier = array.specifier();
        }
        return (GlslStructSpecifier) specifier;
    }

    @Override
    public String getSourceString() {
        if (this.specifiedType.getQualifiers().isEmpty()) {
            return "struct " + this.specifiedType.getSpecifier().getSourceString();
        }
        return this.specifiedType.getSourceString();
    }

    @Override
    public Stream<GlslNode> stream() {
        return Stream.of(this);
    }

    public GlslStructNode setSpecifiedType(GlslSpecifiedType specifiedType) {
        if (!specifiedType.getSpecifier().isStruct()) {
            throw new IllegalArgumentException("specified type must be struct or array of structs");
        }
        this.specifiedType = specifiedType;
        return this;
    }

    @Override
    public @Nullable String getName() {
        return this.specifiedType.getSpecifier().getSourceString();
    }

    @Override
    public GlslStructNode setName(@Nullable String name) {
        GlslTypeSpecifier specifier = this.specifiedType.getSpecifier();
        while (specifier instanceof GlslTypeSpecifier.Array array) {
            specifier = array.specifier();
        }
        ((GlslStructSpecifier) specifier).setName(Objects.requireNonNull(name));
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        GlslStructNode that = (GlslStructNode) o;
        return this.specifiedType.equals(that.specifiedType);
    }

    @Override
    public int hashCode() {
        return this.specifiedType.hashCode();
    }

    @Override
    public String toString() {
        return "GlslStructNode{specifiedType=" + this.specifiedType + '}';
    }
}
