package foundry.veil.impl.glsl.grammar;

import foundry.veil.impl.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

public sealed interface GlslTypeSpecifier extends GlslType permits GlslStructSpecifier, GlslTypeSpecifier.Array, GlslTypeSpecifier.BuiltinType, GlslTypeSpecifier.Name, GlslTypeSpecifier.Struct {

    String getSourceString();

    default String getPostSourceString() {
        return "";
    }

    default boolean isNamed() {
        return this instanceof Name;
    }

    static GlslTypeSpecifier named(String name) {
        return new Name(name);
    }

    static GlslTypeSpecifier array(GlslTypeSpecifier specifier, @Nullable GlslNode size) {
        return new Array(specifier, size);
    }

    @Override
    default GlslSpecifiedType asSpecifiedType() {
        return new GlslSpecifiedType(this);
    }

    record Name(String name) implements GlslTypeSpecifier {
        @Override
        public String getSourceString() {
            return this.name;
        }
    }

    record Array(GlslTypeSpecifier specifier, @Nullable GlslNode size) implements GlslTypeSpecifier {
        @Override
        public String getSourceString() {
            return this.specifier.getSourceString();
        }

        @Override
        public String getPostSourceString() {
            return this.size != null ? "[" + this.size.getSourceString() + "]" : "[]";
        }

        @Override
        public boolean isNamed() {
            return this.specifier.isNamed();
        }
    }

    record Struct(GlslStructSpecifier structSpecifier) implements GlslTypeSpecifier {
        @Override
        public String getSourceString() {
            return this.structSpecifier.getSourceString();
        }

        @Override
        public boolean isNamed() {
            return false;
        }
    }

    enum BuiltinType implements GlslTypeSpecifier {
        VOID("void"),
        FLOAT("float"),
        DOUBLE("double"),
        INT("int"),
        UINT("uint"),
        BOOL("bool"),
        VEC2("vec2"),
        VEC3("vec3"),
        VEC4("vec4"),
        DVEC2("dvec2"),
        DVEC3("dvec3"),
        DVEC4("dvec4"),
        BVEC2("bvec2"),
        BVEC3("bvec3"),
        BVEC4("bvec4"),
        IVEC2("ivec2"),
        IVEC3("ivec3"),
        IVEC4("ivec4"),
        UVEC2("uvec2"),
        UVEC3("uvec3"),
        UVEC4("uvec4"),
        MAT2("mat2"),
        MAT3("mat3"),
        MAT4("mat4"),
        MAT2X2("mat2x2"),
        MAT2X3("mat2x3"),
        MAT2X4("mat2x4"),
        MAT3X2("mat3x2"),
        MAT3X3("mat3x3"),
        MAT3X4("mat3x4"),
        MAT4X2("mat4x2"),
        MAT4X3("mat4x3"),
        MAT4X4("mat4x4"),
        DMAT2("dmat2"),
        DMAT3("dmat3"),
        DMAT4("dmat4"),
        DMAT2X2("dmat2x2"),
        DMAT2X3("dmat2x3"),
        DMAT2X4("dmat2x4"),
        DMAT3X2("dmat3x2"),
        DMAT3X3("dmat3x3"),
        DMAT3X4("dmat3x4"),
        DMAT4X2("dmat4x2"),
        DMAT4X3("dmat4x3"),
        DMAT4X4("dmat4x4"),
        ATOMIC_UINT("atomic_uint"),
        SAMPLER2D("sampler2D"),
        SAMPLER3D("sampler3D"),
        SAMPLERCUBE("samplerCube"),
        SAMPLER2DSHADOW("sampler2DShadow"),
        SAMPLERCUBESHADOW("samplerCubeShadow"),
        SAMPLER2DARRAY("sampler2DArray"),
        SAMPLER2DARRAYSHADOW("sampler2DArrayShadow"),
        SAMPLERCUBEARRAY("samplerCubeArray"),
        SAMPLERCUBEARRAYSHADOW("samplerCubeArrayShadow"),
        ISAMPLER2D("isampler2D"),
        ISAMPLER3D("isampler3D"),
        ISAMPLERCUBE("isamplerCube"),
        ISAMPLER2DARRAY("isampler2DArray"),
        ISAMPLERCUBEARRAY("isamplerCubeArray"),
        USAMPLER2D("usampler2D"),
        USAMPLER3D("usampler3D"),
        USAMPLERCUBE("usamplerCube"),
        USAMPLER2DARRAY("usampler2DArray"),
        USAMPLERCUBEARRAY("usamplerCubeArray"),
        SAMPLER1D("sampler1D"),
        SAMPLER1DSHADOW("sampler1DShadow"),
        SAMPLER1DARRAY("sampler1DArray"),
        SAMPLER1DARRAYSHADOW("sampler1DArrayShadow"),
        ISAMPLER1D("isampler1D"),
        ISAMPLER1DARRAY("isampler1DArray"),
        USAMPLER1D("usampler1D"),
        USAMPLER1DARRAY("usampler1DArray"),
        SAMPLER2DRECT("sampler2DRect"),
        SAMPLER2DRECTSHADOW("sampler2DRectShadow"),
        ISAMPLER2DRECT("isampler2DRect"),
        USAMPLER2DRECT("usampler2DRect"),
        SAMPLERBUFFER("samplerBuffer"),
        ISAMPLERBUFFER("isamplerBuffer"),
        USAMPLERBUFFER("usamplerBuffer"),
        SAMPLER2DMS("sampler2DMS"),
        ISAMPLER2DMS("isampler2DMS"),
        USAMPLER2DMS("usampler2DMS"),
        SAMPLER2DMSARRAY("sampler2DMSArray"),
        ISAMPLER2DMSARRAY("isampler2DMSArray"),
        USAMPLER2DMSARRAY("usampler2DMSArray"),
        IMAGE2D("image2D"),
        IIMAGE2D("iimage2D"),
        UIMAGE2D("uimage2D"),
        IMAGE3D("image3D"),
        IIMAGE3D("iimage3D"),
        UIMAGE3D("uimage3D"),
        IMAGECUBE("imageCube"),
        IIMAGECUBE("iimageCube"),
        UIMAGECUBE("uimageCube"),
        IMAGEBUFFER("imageBuffer"),
        IIMAGEBUFFER("iimageBuffer"),
        UIMAGEBUFFER("uimageBuffer"),
        IMAGE1D("image1D"),
        IIMAGE1D("iimage1D"),
        UIMAGE1D("uimage1D"),
        IMAGE1DARRAY("image1DArray"),
        IIMAGE1DARRAY("iimage1DArray"),
        UIMAGE1DARRAY("uimage1DArray"),
        IMAGE2DRECT("image2DRect"),
        IIMAGE2DRECT("iimage2DRect"),
        UIMAGE2DRECT("uimage2DRect"),
        IMAGE2DARRAY("image2DArray"),
        IIMAGE2DARRAY("iimage2DArray"),
        UIMAGE2DARRAY("uimage2DArray"),
        IMAGECUBEARRAY("imageCubeArray"),
        IIMAGECUBEARRAY("iimageCubeArray"),
        UIMAGECUBEARRAY("uimageCubeArray"),
        IMAGE2DMS("image2DMS"),
        IIMAGE2DMS("iimage2DMS"),
        UIMAGE2DMS("uimage2DMS"),
        IMAGE2DMSARRAY("image2DMSArray"),
        IIMAGE2DMSARRAY("iimage2DMSArray"),
        UIMAGE2DMSARRAY("uimage2DMSArray");

        private final String source;

        BuiltinType(String source) {
            this.source = source;
        }

        @Override
        public String getSourceString() {
            return this.source;
        }
    }
}
