package foundry.veil.impl.glsl;

import foundry.veil.impl.glsl.grammar.GlslTypeQualifier;
import foundry.veil.impl.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.impl.glsl.node.expression.GlslAssignmentNode;
import foundry.veil.impl.glsl.node.expression.GlslUnaryNode;
import gg.moonflower.molangcompiler.core.compiler.StringReader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public final class GlslLexer {

    public static Token[] createTokens(String input) throws GlslSyntaxException {
        return createTokens(input, null);
    }

    public static Token[] createTokens(String input, @Nullable BiConsumer<Integer, Token> commentConsumer) throws GlslSyntaxException {
        StringReader reader = new StringReader(input);
        List<Token> tokens = new ArrayList<>();

        reader.skipWhitespace();
        while (reader.canRead()) {
            Token token = getToken(reader);
            if (token != null) {
                if (token.type != TokenType.COMMENT && token.type != TokenType.MULTI_COMMENT) {
                    tokens.add(token);
                } else if (commentConsumer != null) {
                    commentConsumer.accept(tokens.size(), token);
                }
                reader.skipWhitespace();
                continue;
            }

            throw new GlslSyntaxException("Unknown Token", reader.getString(), reader.getCursor() + 1);
        }

        return tokens.toArray(Token[]::new);
    }

    private static @Nullable Token getToken(StringReader reader) {
        String word = reader.getString().substring(reader.getCursor());
        Token longest = null;
        int length = 0;
        for (TokenType type : TokenType.values()) {
            Matcher matcher = type.pattern.matcher(word);
            if (matcher.find() && matcher.start() == 0 && matcher.end() > length) {
                length = matcher.end();
                longest = new Token(type, word.substring(0, length));
            }
        }

        if (longest != null) {
            reader.setCursor(reader.getCursor() + length);
            return longest;
        }

        return null;
    }

    public record Token(TokenType type, String value) {
        @Override
        public String toString() {
            return this.type + "[" + this.value + "]";
        }
    }

    public enum TokenType {
        DIRECTIVE("#.*"),
        GLSL_MACRO("__LINE__|__FILE__|__VERSION__"),
        COMMENT("\\/\\/.*"),
        MULTI_COMMENT("\\/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*\\/"),

        CONST("const"),
        BOOL("bool"),
        FLOAT("float"),
        INT("int"),
        UINT("uint"),
        DOUBLE("double"),

        BVEC2("bvec2"),
        BVEC3("bvec3"),
        BVEC4("bvec4"),
        IVEC2("ivec2"),
        IVEC3("ivec3"),
        IVEC4("ivec4"),
        UVEC2("uvec2"),
        UVEC3("uvec3"),
        UVEC4("uvec4"),
        VEC2("vec2"),
        VEC3("vec3"),
        VEC4("vec4"),

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

        DVEC2("dvec2"),
        DVEC3("dvec3"),
        DVEC4("dvec4"),
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

        CENTROID("centroid"),
        IN("in"),
        OUT("out"),
        INOUT("inout"),
        UNIFORM("uniform"),
        PATCH("patch"),
        SAMPLE("sample"),
        BUFFER("buffer"),
        SHARED("shared"),
        COHERENT("cohent"),
        VOLATILE("volatile"),
        RESTRICT("restrict"),
        READONLY("readonly"),
        WRITEONLY("writeonly"),
        NOPERSPECTIVE("noperspective"),
        FLAT("flat"),
        SMOOTH("smooth"),
        LAYOUT("layout"),

        ATOMIC_UINT("atomic_uint"),

        SAMPLER2D("sampler2D"),
        SAMPLER3D("sampler3D"),
        SAMPLERCUBE("samplerCube"),
        SAMPLER2DSHADOW("sampler2DShadow"),
        SAMPLERCUBESHADOW("samplerCubeShadow"),
        SAMPLER2DARRAY("sampler2DArray"),
        SAMPLER2DARRAYSHADOW("sampler2DArrayShadow"),
        ISAMPLER2D("isampler2D"),
        ISAMPLER3D("isampler3D"),
        ISAMPLERCUBE("isamplerCube"),
        ISAMPLER2DARRAY("isampler2DArray"),
        USAMPLER2D("usampler2D"),
        USAMPLER3D("usampler3D"),
        USAMPLERCUBE("usamplerCube"),
        USAMPLER2DARRAY("uSampler2DArray"),

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
        SAMPLERCUBEARRAY("samplerCubeArray"),
        SAMPLERCUBEARRAYSHADOW("samplerCubeArrayShadow"),
        ISAMPLERCUBEARRAY("isamplerCubeArray"),
        USAMPLERCUBEARRAY("usamplerCubeArray"),
        SAMPLER2DMS("sampler2Dms"),
        ISAMPLER2DMS("isampler2Dms"),
        USAMPLER2DMS("usampler2Dms"),
        SAMPLER2DMSARRAY("sampler2DMSArray"),
        ISAMPLER2DMSARRAY("isampler2DMSArray"),
        USAMPLER2DMSARRAY("usampler2DMSArray"),
        IMAGE2D("image2D"),
        IIMAGE2D("iimage2D"),
        UIMAGE2D("uimage2D"),
        IMAGE3D("image3D"),
        IIMAGE3D("iimage3D"),
        UIMAGE3D("uimage3D"),
        IMAGECUBE("imagecube"),
        IIMAGECUBE("iimageCube"),
        UIMAGECUBE("uimageCube"),
        IMAGEBUFFER("imageBuffer"),
        IIMAGEBUFFER("iimageBuffer"),
        UIMAGEBUFFER("uimageBuffer"),
        IMAGE2DARRAY("image2DArray"),
        IIMAGE2DARRAY("iimage2DArray"),
        UIMAGE2DARRAY("uimage2DArray"),
        IMAGECUBEARRAY("imagecubeArray"),
        IIMAGECUBEARRAY("iimageCubeArray"),
        UIMAGECUBEARRAY("uimageCubeArray"),

        IMAGE1D("image1D"),
        IIMAGE1D("iimage1D"),
        UIMAGE1D("uimage1D"),
        IMAGE1DARRAY("image1DArray"),
        IIMAGE1DARRAY("iimage1DArray"),
        UIMAGE1DARRAY("uimage1DArray"),
        IMAGE2DRECT("image2DRect"),
        IIMAGE2DRECT("iimage2DRect"),
        UIMAGE2DRECT("uimage2DRect"),
        IMAGE2DMS("image2Dms"),
        IIMAGE2DMS("iimage2DMS"),
        UIMAGE2DMS("uimage2DMS"),
        IMAGE2DMSARRAY("image2DMSArray"),
        IIMAGE2DMSARRAY("iimage2DMSArray"),
        UIMAGE2DMSARRAY("uimage2DMSArray"),

        STRUCT("struct"),
        VOID("void"),

        WHILE("while"),
        BREAK("break"),
        CONTINUE("continue"),
        DO("do"),
        ELSE("else"),
        FOR("for"),
        IF("if"),
        DISCARD("discard"),
        RETURN("return"),
        SWITCH("switch"),
        CASE("case"),
        DEFAULT("default"),
        SUBROUTINE("subroutine"),

        // TYPE_NAME ??
        FLOATING_CONSTANT("(?:(?:\\d+\\.\\d+|\\d+\\.|\\.\\d+)(?:[eE][+-]?\\d+)?(?:f|F|lf|LF)?)|(?:\\d+)(?:\\.|[eE][+-]?\\d+)(?:f|F|lf|LF)?"),
        UINTEGER_HEXADECIMAL_CONSTANT("0[xX][0-9a-fA-F]*[uU]?"),
        UINTEGER_OCTAL_CONSTANT("0[0-7]*[uU]?"),
        UINTEGER_DECIMAL_CONSTANT("[1-9][\\d]*[uU]?"),
        INTEGER_HEXADECIMAL_CONSTANT("0[xX][0-9a-fA-F]*"),
        INTEGER_OCTAL_CONSTANT("0[0-7]*"),
        INTEGER_DECIMAL_CONSTANT("[1-9][\\d]*"),
        BOOL_CONSTANT("true|false"),
        // FIELD_SELECTION

        LEFT_OP("<<"),
        RIGHT_OP(">>"),
        INC_OP("\\+\\+"),
        DEC_OP("--"),
        LE_OP("<="),
        GE_OP(">="),
        EQ_OP("=="),
        NE_OP("!="),
        AND_OP("&&"),
        OR_OP("\\|\\|"),
        XOR_OP("\\^\\^"),
        MUL_ASSIGN("\\*="),
        DIV_ASSIGN("\\/="),
        ADD_ASSIGN("\\+="),
        MOD_ASSIGN("%="),
        LEFT_ASSIGN("<<="),
        RIGHT_ASSIGN(">>="),
        AND_ASSIGN("&="),
        XOR_ASSIGN("\\^="),
        OR_ASSIGN("\\|="),
        SUB_ASSIGN("-="),
        LEFT_PAREN("\\("),
        RIGHT_PAREN("\\)"),
        LEFT_BRACKET("\\["),
        RIGHT_BRACKET("\\]"),
        LEFT_BRACE("\\{"),
        RIGHT_BRACE("\\}"),
        DOT("\\."),
        COMMA(","),
        COLON(":"),
        EQUAL("="),
        SEMICOLON(";"),
        BANG("!"),
        DASH("-"),
        TILDE("~"),
        PLUS("\\+"),
        STAR("\\*"),
        SLASH("\\/"),
        PERCENT("%"),
        LEFT_ANGLE("<"),
        RIGHT_ANGLE(">"),
        VERTICAL_BAR("\\|"),
        CARET("\\^"),
        AMPERSAND("&"),
        QUESTION("\\?"),

        INVARIANT("invariant"),
        PRECISE("precise"),
        HIGH_PRECISION("highp"),
        MEDIUM_PRECISION("mediump"),
        LOW_PRECISION("lowp"),
        PRECISION("precision"),

        IDENTIFIER("[_a-zA-Z][\\d_a-zA-Z]*");

        private final Pattern pattern;

        TokenType(String regex) {
            this.pattern = Pattern.compile(regex);
        }

        public @Nullable GlslTypeSpecifier.BuiltinType asBuiltinType() {
            return switch (this) {
                case VOID -> GlslTypeSpecifier.BuiltinType.VOID;
                case FLOAT -> GlslTypeSpecifier.BuiltinType.FLOAT;
                case DOUBLE -> GlslTypeSpecifier.BuiltinType.DOUBLE;
                case INT -> GlslTypeSpecifier.BuiltinType.INT;
                case UINT -> GlslTypeSpecifier.BuiltinType.UINT;
                case BOOL -> GlslTypeSpecifier.BuiltinType.BOOL;
                case VEC2 -> GlslTypeSpecifier.BuiltinType.VEC2;
                case VEC3 -> GlslTypeSpecifier.BuiltinType.VEC3;
                case VEC4 -> GlslTypeSpecifier.BuiltinType.VEC4;
                case DVEC2 -> GlslTypeSpecifier.BuiltinType.DVEC2;
                case DVEC3 -> GlslTypeSpecifier.BuiltinType.DVEC3;
                case DVEC4 -> GlslTypeSpecifier.BuiltinType.DVEC4;
                case BVEC2 -> GlslTypeSpecifier.BuiltinType.BVEC2;
                case BVEC3 -> GlslTypeSpecifier.BuiltinType.BVEC3;
                case BVEC4 -> GlslTypeSpecifier.BuiltinType.BVEC4;
                case IVEC2 -> GlslTypeSpecifier.BuiltinType.IVEC2;
                case IVEC3 -> GlslTypeSpecifier.BuiltinType.IVEC3;
                case IVEC4 -> GlslTypeSpecifier.BuiltinType.IVEC4;
                case UVEC2 -> GlslTypeSpecifier.BuiltinType.UVEC2;
                case UVEC3 -> GlslTypeSpecifier.BuiltinType.UVEC3;
                case UVEC4 -> GlslTypeSpecifier.BuiltinType.UVEC4;
                case MAT2 -> GlslTypeSpecifier.BuiltinType.MAT2;
                case MAT3 -> GlslTypeSpecifier.BuiltinType.MAT3;
                case MAT4 -> GlslTypeSpecifier.BuiltinType.MAT4;
                case MAT2X2 -> GlslTypeSpecifier.BuiltinType.MAT2X2;
                case MAT2X3 -> GlslTypeSpecifier.BuiltinType.MAT2X3;
                case MAT2X4 -> GlslTypeSpecifier.BuiltinType.MAT2X4;
                case MAT3X2 -> GlslTypeSpecifier.BuiltinType.MAT3X2;
                case MAT3X3 -> GlslTypeSpecifier.BuiltinType.MAT3X3;
                case MAT3X4 -> GlslTypeSpecifier.BuiltinType.MAT3X4;
                case MAT4X2 -> GlslTypeSpecifier.BuiltinType.MAT4X2;
                case MAT4X3 -> GlslTypeSpecifier.BuiltinType.MAT4X3;
                case MAT4X4 -> GlslTypeSpecifier.BuiltinType.MAT4X4;
                case DMAT2 -> GlslTypeSpecifier.BuiltinType.DMAT2;
                case DMAT3 -> GlslTypeSpecifier.BuiltinType.DMAT3;
                case DMAT4 -> GlslTypeSpecifier.BuiltinType.DMAT4;
                case DMAT2X2 -> GlslTypeSpecifier.BuiltinType.DMAT2X2;
                case DMAT2X3 -> GlslTypeSpecifier.BuiltinType.DMAT2X3;
                case DMAT2X4 -> GlslTypeSpecifier.BuiltinType.DMAT2X4;
                case DMAT3X2 -> GlslTypeSpecifier.BuiltinType.DMAT3X2;
                case DMAT3X3 -> GlslTypeSpecifier.BuiltinType.DMAT3X3;
                case DMAT3X4 -> GlslTypeSpecifier.BuiltinType.DMAT3X4;
                case DMAT4X2 -> GlslTypeSpecifier.BuiltinType.DMAT4X2;
                case DMAT4X3 -> GlslTypeSpecifier.BuiltinType.DMAT4X3;
                case DMAT4X4 -> GlslTypeSpecifier.BuiltinType.DMAT4X4;
                case ATOMIC_UINT -> GlslTypeSpecifier.BuiltinType.ATOMIC_UINT;
                case SAMPLER2D -> GlslTypeSpecifier.BuiltinType.SAMPLER2D;
                case SAMPLER3D -> GlslTypeSpecifier.BuiltinType.SAMPLER3D;
                case SAMPLERCUBE -> GlslTypeSpecifier.BuiltinType.SAMPLERCUBE;
                case SAMPLER2DSHADOW -> GlslTypeSpecifier.BuiltinType.SAMPLER2DSHADOW;
                case SAMPLERCUBESHADOW -> GlslTypeSpecifier.BuiltinType.SAMPLERCUBESHADOW;
                case SAMPLER2DARRAY -> GlslTypeSpecifier.BuiltinType.SAMPLER2DARRAY;
                case SAMPLER2DARRAYSHADOW -> GlslTypeSpecifier.BuiltinType.SAMPLER2DARRAYSHADOW;
                case SAMPLERCUBEARRAY -> GlslTypeSpecifier.BuiltinType.SAMPLERCUBEARRAY;
                case SAMPLERCUBEARRAYSHADOW -> GlslTypeSpecifier.BuiltinType.SAMPLERCUBEARRAYSHADOW;
                case ISAMPLER2D -> GlslTypeSpecifier.BuiltinType.ISAMPLER2D;
                case ISAMPLER3D -> GlslTypeSpecifier.BuiltinType.ISAMPLER3D;
                case ISAMPLERCUBE -> GlslTypeSpecifier.BuiltinType.ISAMPLERCUBE;
                case ISAMPLER2DARRAY -> GlslTypeSpecifier.BuiltinType.ISAMPLER2DARRAY;
                case ISAMPLERCUBEARRAY -> GlslTypeSpecifier.BuiltinType.ISAMPLERCUBEARRAY;
                case USAMPLER2D -> GlslTypeSpecifier.BuiltinType.USAMPLER2D;
                case USAMPLER3D -> GlslTypeSpecifier.BuiltinType.USAMPLER3D;
                case USAMPLERCUBE -> GlslTypeSpecifier.BuiltinType.USAMPLERCUBE;
                case USAMPLER2DARRAY -> GlslTypeSpecifier.BuiltinType.USAMPLER2DARRAY;
                case USAMPLERCUBEARRAY -> GlslTypeSpecifier.BuiltinType.USAMPLERCUBEARRAY;
                case SAMPLER1D -> GlslTypeSpecifier.BuiltinType.SAMPLER1D;
                case SAMPLER1DSHADOW -> GlslTypeSpecifier.BuiltinType.SAMPLER1DSHADOW;
                case SAMPLER1DARRAY -> GlslTypeSpecifier.BuiltinType.SAMPLER1DARRAY;
                case SAMPLER1DARRAYSHADOW -> GlslTypeSpecifier.BuiltinType.SAMPLER1DARRAYSHADOW;
                case ISAMPLER1D -> GlslTypeSpecifier.BuiltinType.ISAMPLER1D;
                case ISAMPLER1DARRAY -> GlslTypeSpecifier.BuiltinType.ISAMPLER1DARRAY;
                case USAMPLER1D -> GlslTypeSpecifier.BuiltinType.USAMPLER1D;
                case USAMPLER1DARRAY -> GlslTypeSpecifier.BuiltinType.USAMPLER1DARRAY;
                case SAMPLER2DRECT -> GlslTypeSpecifier.BuiltinType.SAMPLER2DRECT;
                case SAMPLER2DRECTSHADOW -> GlslTypeSpecifier.BuiltinType.SAMPLER2DRECTSHADOW;
                case ISAMPLER2DRECT -> GlslTypeSpecifier.BuiltinType.ISAMPLER2DRECT;
                case USAMPLER2DRECT -> GlslTypeSpecifier.BuiltinType.USAMPLER2DRECT;
                case SAMPLERBUFFER -> GlslTypeSpecifier.BuiltinType.SAMPLERBUFFER;
                case ISAMPLERBUFFER -> GlslTypeSpecifier.BuiltinType.ISAMPLERBUFFER;
                case USAMPLERBUFFER -> GlslTypeSpecifier.BuiltinType.USAMPLERBUFFER;
                case SAMPLER2DMS -> GlslTypeSpecifier.BuiltinType.SAMPLER2DMS;
                case ISAMPLER2DMS -> GlslTypeSpecifier.BuiltinType.ISAMPLER2DMS;
                case USAMPLER2DMS -> GlslTypeSpecifier.BuiltinType.USAMPLER2DMS;
                case SAMPLER2DMSARRAY -> GlslTypeSpecifier.BuiltinType.SAMPLER2DMSARRAY;
                case ISAMPLER2DMSARRAY -> GlslTypeSpecifier.BuiltinType.ISAMPLER2DMSARRAY;
                case USAMPLER2DMSARRAY -> GlslTypeSpecifier.BuiltinType.USAMPLER2DMSARRAY;
                case IMAGE2D -> GlslTypeSpecifier.BuiltinType.IMAGE2D;
                case IIMAGE2D -> GlslTypeSpecifier.BuiltinType.IIMAGE2D;
                case UIMAGE2D -> GlslTypeSpecifier.BuiltinType.UIMAGE2D;
                case IMAGE3D -> GlslTypeSpecifier.BuiltinType.IMAGE3D;
                case IIMAGE3D -> GlslTypeSpecifier.BuiltinType.IIMAGE3D;
                case UIMAGE3D -> GlslTypeSpecifier.BuiltinType.UIMAGE3D;
                case IMAGECUBE -> GlslTypeSpecifier.BuiltinType.IMAGECUBE;
                case IIMAGECUBE -> GlslTypeSpecifier.BuiltinType.IIMAGECUBE;
                case UIMAGECUBE -> GlslTypeSpecifier.BuiltinType.UIMAGECUBE;
                case IMAGEBUFFER -> GlslTypeSpecifier.BuiltinType.IMAGEBUFFER;
                case IIMAGEBUFFER -> GlslTypeSpecifier.BuiltinType.IIMAGEBUFFER;
                case UIMAGEBUFFER -> GlslTypeSpecifier.BuiltinType.UIMAGEBUFFER;
                case IMAGE1D -> GlslTypeSpecifier.BuiltinType.IMAGE1D;
                case IIMAGE1D -> GlslTypeSpecifier.BuiltinType.IIMAGE1D;
                case UIMAGE1D -> GlslTypeSpecifier.BuiltinType.UIMAGE1D;
                case IMAGE1DARRAY -> GlslTypeSpecifier.BuiltinType.IMAGE1DARRAY;
                case IIMAGE1DARRAY -> GlslTypeSpecifier.BuiltinType.IIMAGE1DARRAY;
                case UIMAGE1DARRAY -> GlslTypeSpecifier.BuiltinType.UIMAGE1DARRAY;
                case IMAGE2DRECT -> GlslTypeSpecifier.BuiltinType.IMAGE2DRECT;
                case IIMAGE2DRECT -> GlslTypeSpecifier.BuiltinType.IIMAGE2DRECT;
                case UIMAGE2DRECT -> GlslTypeSpecifier.BuiltinType.UIMAGE2DRECT;
                case IMAGE2DARRAY -> GlslTypeSpecifier.BuiltinType.IMAGE2DARRAY;
                case IIMAGE2DARRAY -> GlslTypeSpecifier.BuiltinType.IIMAGE2DARRAY;
                case UIMAGE2DARRAY -> GlslTypeSpecifier.BuiltinType.UIMAGE2DARRAY;
                case IMAGECUBEARRAY -> GlslTypeSpecifier.BuiltinType.IMAGECUBEARRAY;
                case IIMAGECUBEARRAY -> GlslTypeSpecifier.BuiltinType.IIMAGECUBEARRAY;
                case UIMAGECUBEARRAY -> GlslTypeSpecifier.BuiltinType.UIMAGECUBEARRAY;
                case IMAGE2DMS -> GlslTypeSpecifier.BuiltinType.IMAGE2DMS;
                case IIMAGE2DMS -> GlslTypeSpecifier.BuiltinType.IIMAGE2DMS;
                case UIMAGE2DMS -> GlslTypeSpecifier.BuiltinType.UIMAGE2DMS;
                case IMAGE2DMSARRAY -> GlslTypeSpecifier.BuiltinType.IMAGE2DMSARRAY;
                case IIMAGE2DMSARRAY -> GlslTypeSpecifier.BuiltinType.IIMAGE2DMSARRAY;
                case UIMAGE2DMSARRAY -> GlslTypeSpecifier.BuiltinType.UIMAGE2DMSARRAY;
                default -> null;
            };
        }

        public @Nullable GlslTypeQualifier.StorageType asStorageQualifier() {
            return switch (this) {
                case CONST -> GlslTypeQualifier.StorageType.CONST;
                case IN -> GlslTypeQualifier.StorageType.IN;
                case OUT -> GlslTypeQualifier.StorageType.OUT;
                case INOUT -> GlslTypeQualifier.StorageType.INOUT;
                case CENTROID -> GlslTypeQualifier.StorageType.CENTROID;
                case PATCH -> GlslTypeQualifier.StorageType.PATCH;
                case SAMPLE -> GlslTypeQualifier.StorageType.SAMPLE;
                case UNIFORM -> GlslTypeQualifier.StorageType.UNIFORM;
                case BUFFER -> GlslTypeQualifier.StorageType.BUFFER;
                case SHARED -> GlslTypeQualifier.StorageType.SHARED;
                case COHERENT -> GlslTypeQualifier.StorageType.COHERENT;
                case VOLATILE -> GlslTypeQualifier.StorageType.VOLATILE;
                case RESTRICT -> GlslTypeQualifier.StorageType.RESTRICT;
                case READONLY -> GlslTypeQualifier.StorageType.READONLY;
                case WRITEONLY -> GlslTypeQualifier.StorageType.WRITEONLY;
                default -> null;
            };
        }

        public @Nullable GlslTypeQualifier.Precision asPrecisionQualifier() {
            return switch (this) {
                case HIGH_PRECISION -> GlslTypeQualifier.Precision.HIGH_PRECISION;
                case MEDIUM_PRECISION -> GlslTypeQualifier.Precision.MEDIUM_PRECISION;
                case LOW_PRECISION -> GlslTypeQualifier.Precision.LOW_PRECISION;
                default -> null;
            };
        }

        public @Nullable GlslTypeQualifier.Interpolation asInterpolationQualifier() {
            return switch (this) {
                case SMOOTH -> GlslTypeQualifier.Interpolation.SMOOTH;
                case FLAT -> GlslTypeQualifier.Interpolation.FLAT;
                case NOPERSPECTIVE -> GlslTypeQualifier.Interpolation.NOPERSPECTIVE;
                default -> null;
            };
        }

        public @Nullable GlslAssignmentNode.Operand asAssignmentOperator() {
            return switch (this) {
                case EQUAL -> GlslAssignmentNode.Operand.EQUAL;
                case MUL_ASSIGN -> GlslAssignmentNode.Operand.MUL_ASSIGN;
                case DIV_ASSIGN -> GlslAssignmentNode.Operand.DIV_ASSIGN;
                case MOD_ASSIGN -> GlslAssignmentNode.Operand.MOD_ASSIGN;
                case ADD_ASSIGN -> GlslAssignmentNode.Operand.ADD_ASSIGN;
                case SUB_ASSIGN -> GlslAssignmentNode.Operand.SUB_ASSIGN;
                case LEFT_ASSIGN -> GlslAssignmentNode.Operand.LEFT_ASSIGN;
                case RIGHT_ASSIGN -> GlslAssignmentNode.Operand.RIGHT_ASSIGN;
                case AND_ASSIGN -> GlslAssignmentNode.Operand.AND_ASSIGN;
                case XOR_ASSIGN -> GlslAssignmentNode.Operand.XOR_ASSIGN;
                case OR_ASSIGN -> GlslAssignmentNode.Operand.OR_ASSIGN;
                default -> null;
            };
        }

        public @Nullable GlslUnaryNode.Operand asUnaryOperator() {
            return switch (this) {
                case PLUS -> GlslUnaryNode.Operand.PLUS;
                case DASH -> GlslUnaryNode.Operand.DASH;
                case BANG -> GlslUnaryNode.Operand.BANG;
                case TILDE -> GlslUnaryNode.Operand.TILDE;
                default -> null;
            };
        }
    }
}
