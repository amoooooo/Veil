package foundry.veil.api.glsl.node.variable;

import foundry.veil.api.glsl.grammar.GlslSpecifiedType;
import foundry.veil.api.glsl.grammar.GlslStructSpecifier;
import foundry.veil.api.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.api.glsl.node.GlslNode;

import java.util.stream.Stream;

public class GlslStructNode implements GlslNode {

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
}
