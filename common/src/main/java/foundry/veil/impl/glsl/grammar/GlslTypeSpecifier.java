package foundry.veil.impl.glsl.grammar;

import foundry.veil.impl.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

public interface GlslTypeSpecifier extends GlslType {

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
    }

    record Array(GlslTypeSpecifier specifier, @Nullable GlslNode size) implements GlslTypeSpecifier {
        @Override
        public boolean isNamed() {
            return this.specifier.isNamed();
        }
    }

    enum BuiltinType implements GlslTypeSpecifier {
        VOID,
        FLOAT,
        DOUBLE,
        INT,
        UINT,
        BOOL,
        VEC2,
        VEC3,
        VEC4,
        DVEC2,
        DVEC3,
        DVEC4,
        BVEC2,
        BVEC3,
        BVEC4,
        IVEC2,
        IVEC3,
        IVEC4,
        UVEC2,
        UVEC3,
        UVEC4,
        MAT2,
        MAT3,
        MAT4,
        MAT2X2,
        MAT2X3,
        MAT2X4,
        MAT3X2,
        MAT3X3,
        MAT3X4,
        MAT4X2,
        MAT4X3,
        MAT4X4,
        DMAT2,
        DMAT3,
        DMAT4,
        DMAT2X2,
        DMAT2X3,
        DMAT2X4,
        DMAT3X2,
        DMAT3X3,
        DMAT3X4,
        DMAT4X2,
        DMAT4X3,
        DMAT4X4,
        ATOMIC_UINT,
        SAMPLER2D,
        SAMPLER3D,
        SAMPLERCUBE,
        SAMPLER2DSHADOW,
        SAMPLERCUBESHADOW,
        SAMPLER2DARRAY,
        SAMPLER2DARRAYSHADOW,
        SAMPLERCUBEARRAY,
        SAMPLERCUBEARRAYSHADOW,
        ISAMPLER2D,
        ISAMPLER3D,
        ISAMPLERCUBE,
        ISAMPLER2DARRAY,
        ISAMPLERCUBEARRAY,
        USAMPLER2D,
        USAMPLER3D,
        USAMPLERCUBE,
        USAMPLER2DARRAY,
        USAMPLERCUBEARRAY,
        SAMPLER1D,
        SAMPLER1DSHADOW,
        SAMPLER1DARRAY,
        SAMPLER1DARRAYSHADOW,
        ISAMPLER1D,
        ISAMPLER1DARRAY,
        USAMPLER1D,
        USAMPLER1DARRAY,
        SAMPLER2DRECT,
        SAMPLER2DRECTSHADOW,
        ISAMPLER2DRECT,
        USAMPLER2DRECT,
        SAMPLERBUFFER,
        ISAMPLERBUFFER,
        USAMPLERBUFFER,
        SAMPLER2DMS,
        ISAMPLER2DMS,
        USAMPLER2DMS,
        SAMPLER2DMSARRAY,
        ISAMPLER2DMSARRAY,
        USAMPLER2DMSARRAY,
        IMAGE2D,
        IIMAGE2D,
        UIMAGE2D,
        IMAGE3D,
        IIMAGE3D,
        UIMAGE3D,
        IMAGECUBE,
        IIMAGECUBE,
        UIMAGECUBE,
        IMAGEBUFFER,
        IIMAGEBUFFER,
        UIMAGEBUFFER,
        IMAGE1D,
        IIMAGE1D,
        UIMAGE1D,
        IMAGE1DARRAY,
        IIMAGE1DARRAY,
        UIMAGE1DARRAY,
        IMAGE2DRECT,
        IIMAGE2DRECT,
        UIMAGE2DRECT,
        IMAGE2DARRAY,
        IIMAGE2DARRAY,
        UIMAGE2DARRAY,
        IMAGECUBEARRAY,
        IIMAGECUBEARRAY,
        UIMAGECUBEARRAY,
        IMAGE2DMS,
        IIMAGE2DMS,
        UIMAGE2DMS,
        IMAGE2DMSARRAY,
        IIMAGE2DMSARRAY,
        UIMAGE2DMSARRAY
    }
}
