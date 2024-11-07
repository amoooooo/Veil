import foundry.veil.impl.glsl.GlslTokenReader;
import foundry.veil.impl.glsl.node.GlslNode;
import org.jetbrains.annotations.Nullable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public class GlslGrammarGenerator {

    private static final String RAW = """
            variable_identifier :
            IDENTIFIER
            
            primary_expression :
            variable_identifier
            INTCONSTANT
            UINTCONSTANT
            FLOATCONSTANT
            BOOLCONSTANT
            DOUBLECONSTANT
            LEFT_PAREN expression RIGHT_PAREN
            
            postfix_expression :
            primary_expression
            postfix_expression LEFT_BRACKET integer_expression RIGHT_BRACKET
            function_call
            postfix_expression DOT FIELD_SELECTION
            postfix_expression INC_OP
            postfix_expression DEC_OP
            
            // FIELD_SELECTION includes members in structures, component selection for vectors and the 'length' identifier for the length() method
            
            integer_expression :
            expression
            
            function_call :
            function_call_or_method
            
            function_call_or_method :
            function_call_generic
            
            function_call_generic :
            function_call_header_with_parameters RIGHT_PAREN
            function_call_header_no_parameters RIGHT_PAREN
            
            function_call_header_no_parameters :
            function_call_header VOID
            function_call_header
            
            function_call_header_with_parameters :
            function_call_header assignment_expression
            function_call_header_with_parameters COMMA assignment_expression
            
            function_call_header :
            function_identifier LEFT_PAREN
            
            // Methods (.length), subroutine array calls, and identifiers are recognized through postfix_expression.
            
            function_identifier :
            type_specifier
            postfix_expression
            
            unary_expression :
            postfix_expression
            INC_OP unary_expression
            DEC_OP unary_expression
            unary_operator unary_expression
            
            unary_operator :
            PLUS
            DASH
            BANG
            TILDE
            
            multiplicative_expression :
            unary_expression
            multiplicative_expression STAR unary_expression
            multiplicative_expression SLASH unary_expression
            multiplicative_expression PERCENT unary_expression
            
            additive_expression :
            multiplicative_expression
            additive_expression PLUS multiplicative_expression
            additive_expression DASH multiplicative_expression
            
            shift_expression :
            additive_expression
            shift_expression LEFT_OP additive_expression
            shift_expression RIGHT_OP additive_expression
            
            relational_expression :
            shift_expression
            relational_expression LEFT_ANGLE shift_expression
            relational_expression RIGHT_ANGLE shift_expression
            relational_expression LE_OP shift_expression
            relational_expression GE_OP shift_expression
            
            equality_expression :
            relational_expression
            equality_expression EQ_OP relational_expression
            equality_expression NE_OP relational_expression
            
            and_expression :
            equality_expression
            and_expression AMPERSAND equality_expression
            
            exclusive_or_expression :
            and_expression
            exclusive_or_expression CARET and_expression
            
            inclusive_or_expression :
            exclusive_or_expression
            inclusive_or_expression VERTICAL_BAR exclusive_or_expression
            
            logical_and_expression :
            inclusive_or_expression
            logical_and_expression AND_OP inclusive_or_expression
            
            logical_xor_expression :
            logical_and_expression
            logical_xor_expression XOR_OP logical_and_expression
            
            logical_or_expression :
            logical_xor_expression
            logical_or_expression OR_OP logical_xor_expression
            
            conditional_expression :
            logical_or_expression
            logical_or_expression QUESTION expression COLON assignment_expression
            
            assignment_expression :
            conditional_expression
            unary_expression assignment_operator assignment_expression
            
            assignment_operator :
            EQUAL
            MUL_ASSIGN
            DIV_ASSIGN
            MOD_ASSIGN
            ADD_ASSIGN
            SUB_ASSIGN
            LEFT_ASSIGN
            RIGHT_ASSIGN
            AND_ASSIGN
            XOR_ASSIGN
            OR_ASSIGN
            
            expression :
            assignment_expression
            expression COMMA assignment_expression
            
            constant_expression :
            conditional_expression
            
            declaration :
            function_prototype SEMICOLON
            init_declarator_list SEMICOLON
            PRECISION precision_qualifier type_specifier SEMICOLON
            type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE SEMICOLON
            type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE IDENTIFIER SEMICOLON
            type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE IDENTIFIER array_specifier SEMICOLON
            type_qualifier SEMICOLON
            type_qualifier IDENTIFIER SEMICOLON
            type_qualifier IDENTIFIER identifier_list SEMICOLON
            
            identifier_list :
            COMMA IDENTIFIER
            identifier_list COMMA IDENTIFIER
            
            function_prototype :
            function_declarator RIGHT_PAREN
            
            function_declarator :
            function_header
            function_header_with_parameters
            
            function_header_with_parameters :
            function_header parameter_declaration
            function_header_with_parameters COMMA parameter_declaration
            
            function_header :
            fully_specified_type IDENTIFIER LEFT_PAREN
            
            parameter_declarator :
            type_specifier IDENTIFIER
            type_specifier IDENTIFIER array_specifier
            
            parameter_declaration :
            type_qualifier parameter_declarator
            parameter_declarator
            type_qualifier parameter_type_specifier
            parameter_type_specifier
            
            parameter_type_specifier :
            type_specifier
            
            init_declarator_list :
            single_declaration
            init_declarator_list COMMA IDENTIFIER
            init_declarator_list COMMA IDENTIFIER array_specifier
            init_declarator_list COMMA IDENTIFIER array_specifier EQUAL initializer
            init_declarator_list COMMA IDENTIFIER EQUAL initializer
            
            single_declaration :
            fully_specified_type
            fully_specified_type IDENTIFIER
            fully_specified_type IDENTIFIER array_specifier
            fully_specified_type IDENTIFIER array_specifier EQUAL initializer
            fully_specified_type IDENTIFIER EQUAL initializer
            
            fully_specified_type :
            type_specifier
            type_qualifier type_specifier
            
            invariant_qualifier :
            INVARIANT
            
            interpolation_qualifier :
            SMOOTH
            FLAT
            NOPERSPECTIVE
            
            layout_qualifier :
            LAYOUT LEFT_PAREN layout_qualifier_id_list RIGHT_PAREN
            
            layout_qualifier_id_list :
            layout_qualifier_id
            layout_qualifier_id_list COMMA layout_qualifier_id
            
            layout_qualifier_id :
            IDENTIFIER
            IDENTIFIER EQUAL constant_expression
            SHARED
            
            precise_qualifier :
            PRECISE
            
            type_qualifier :
            single_type_qualifier
            type_qualifier single_type_qualifier
            
            single_type_qualifier :
            storage_qualifier
            layout_qualifier
            precision_qualifier
            interpolation_qualifier
            invariant_qualifier
            precise_qualifier
            
            storage_qualifier :
            CONST
            IN
            OUT
            INOUT
            CENTROID
            PATCH
            SAMPLE
            UNIFORM
            BUFFER
            SHARED
            COHERENT
            VOLATILE
            RESTRICT
            READONLY
            WRITEONLY
            SUBROUTINE
            SUBROUTINE LEFT_PAREN type_name_list RIGHT_PAREN
            
            type_name_list :
            TYPE_NAME
            type_name_list COMMA TYPE_NAME
            
            type_specifier :
            type_specifier_nonarray
            type_specifier_nonarray array_specifier
            
            array_specifier :
            LEFT_BRACKET RIGHT_BRACKET
            LEFT_BRACKET conditional_expression RIGHT_BRACKET
            array_specifier LEFT_BRACKET RIGHT_BRACKET
            array_specifier LEFT_BRACKET conditional_expression RIGHT_BRACKET
            
            type_specifier_nonarray :
            VOID
            FLOAT
            DOUBLE
            INT
            UINT
            BOOL
            VEC2
            VEC3
            VEC4
            DVEC2
            DVEC3
            DVEC4
            BVEC2
            BVEC3
            BVEC4
            IVEC2
            IVEC3
            IVEC4
            UVEC2
            UVEC3
            UVEC4
            MAT2
            MAT3
            MAT4
            MAT2X2
            MAT2X3
            MAT2X4
            MAT3X2
            MAT3X3
            MAT3X4
            MAT4X2
            MAT4X3
            MAT4X4
            DMAT2
            DMAT3
            DMAT4
            DMAT2X2
            DMAT2X3
            DMAT2X4
            DMAT3X2
            DMAT3X3
            DMAT3X4
            DMAT4X2
            DMAT4X3
            DMAT4X4
            ATOMIC_UINT
            SAMPLER2D
            SAMPLER3D
            SAMPLERCUBE
            SAMPLER2DSHADOW
            SAMPLERCUBESHADOW
            SAMPLER2DARRAY
            SAMPLER2DARRAYSHADOW
            SAMPLERCUBEARRAY
            SAMPLERCUBEARRAYSHADOW
            ISAMPLER2D
            ISAMPLER3D
            ISAMPLERCUBE
            ISAMPLER2DARRAY
            ISAMPLERCUBEARRAY
            USAMPLER2D
            USAMPLER3D
            USAMPLERCUBE
            USAMPLER2DARRAY
            USAMPLERCUBEARRAY
            SAMPLER1D
            SAMPLER1DSHADOW
            SAMPLER1DARRAY
            SAMPLER1DARRAYSHADOW
            ISAMPLER1D
            ISAMPLER1DARRAY
            USAMPLER1D
            USAMPLER1DARRAY
            SAMPLER2DRECT
            SAMPLER2DRECTSHADOW
            ISAMPLER2DRECT
            USAMPLER2DRECT
            SAMPLERBUFFER
            ISAMPLERBUFFER
            USAMPLERBUFFER
            SAMPLER2DMS
            ISAMPLER2DMS
            USAMPLER2DMS
            SAMPLER2DMSARRAY
            ISAMPLER2DMSARRAY
            USAMPLER2DMSARRAY
            IMAGE2D
            IIMAGE2D
            UIMAGE2D
            IMAGE3D
            IIMAGE3D
            UIMAGE3D
            IMAGECUBE
            IIMAGECUBE
            UIMAGECUBE
            IMAGEBUFFER
            IIMAGEBUFFER
            UIMAGEBUFFER
            IMAGE1D
            IIMAGE1D
            UIMAGE1D
            IMAGE1DARRAY
            IIMAGE1DARRAY
            UIMAGE1DARRAY
            IMAGE2DRECT
            IIMAGE2DRECT
            UIMAGE2DRECT
            IMAGE2DARRAY
            IIMAGE2DARRAY
            UIMAGE2DARRAY
            IMAGECUBEARRAY
            IIMAGECUBEARRAY
            UIMAGECUBEARRAY
            IMAGE2DMS
            IIMAGE2DMS
            UIMAGE2DMS
            IMAGE2DMSARRAY
            IIMAGE2DMSARRAY
            UIMAGE2DMSARRAY
            struct_specifier
            TYPE_NAME
            
            precision_qualifier :
            HIGH_PRECISION
            MEDIUM_PRECISION
            LOW_PRECISION
            
            struct_specifier :
            STRUCT IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE
            STRUCT LEFT_BRACE struct_declaration_list RIGHT_BRACE
            
            struct_declaration_list :
            struct_declaration
            struct_declaration_list struct_declaration
            
            struct_declaration :
            type_specifier struct_declarator_list SEMICOLON
            type_qualifier type_specifier struct_declarator_list SEMICOLON
            
            struct_declarator_list :
            struct_declarator
            struct_declarator_list COMMA struct_declarator
            
            struct_declarator :
            IDENTIFIER
            IDENTIFIER array_specifier
            
            initializer :
            assignment_expression
            LEFT_BRACE initializer_list RIGHT_BRACE
            LEFT_BRACE initializer_list COMMA RIGHT_BRACE
            
            initializer_list :
            initializer
            initializer_list COMMA initializer
            
            declaration_statement :
            declaration
            
            statement :
            compound_statement
            simple_statement
            
            simple_statement :
            declaration_statement
            expression_statement
            selection_statement
            switch_statement
            case_label
            iteration_statement
            jump_statement
            
            compound_statement :
            LEFT_BRACE RIGHT_BRACE
            LEFT_BRACE statement_list RIGHT_BRACE
            
            statement_no_new_scope :
            compound_statement_no_new_scope
            simple_statement
            
            compound_statement_no_new_scope :
            LEFT_BRACE RIGHT_BRACE
            LEFT_BRACE statement_list RIGHT_BRACE
            
            statement_list :
            statement
            statement_list statement
            
            expression_statement :
            SEMICOLON
            expression SEMICOLON
            
            selection_statement :
            IF LEFT_PAREN expression RIGHT_PAREN selection_rest_statement
            
            selection_rest_statement :
            statement ELSE statement
            statement
            
            condition :
            expression
            fully_specified_type IDENTIFIER EQUAL initializer
            
            switch_statement :
            SWITCH LEFT_PAREN expression RIGHT_PAREN LEFT_BRACE switch_statement_list
            RIGHT_BRACE
            
            switch_statement_list :
            /* nothing */
            statement_list
            
            case_label :
            CASE expression COLON
            DEFAULT COLON
            
            iteration_statement :
            WHILE LEFT_PAREN condition RIGHT_PAREN statement_no_new_scope
            DO statement WHILE LEFT_PAREN expression RIGHT_PAREN SEMICOLON
            FOR LEFT_PAREN for_init_statement for_rest_statement RIGHT_PAREN statement_no_new_scope
            
            for_init_statement :
            expression_statement
            declaration_statement
            
            conditionopt :
            condition
            /* empty */
            
            for_rest_statement :
            conditionopt SEMICOLON
            conditionopt SEMICOLON expression
            
            jump_statement :
            CONTINUE SEMICOLON
            BREAK SEMICOLON
            RETURN SEMICOLON
            RETURN expression SEMICOLON
            // Fragment shader only.
            DISCARD SEMICOLON
            
            translation_unit :
            external_declaration
            translation_unit external_declaration
            
            external_declaration :
            function_definition
            declaration
            SEMICOLON
            
            function_definition :
            function_prototype compound_statement_no_new_scope
            """;

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(RAW);

        List<Declaration> declarations = new ArrayList<>();

        String name = null;
        List<String> values = new ArrayList<>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("//") || line.startsWith("/*")) {
                continue;
            }

            if (line.endsWith(" :")) {
                if (!values.isEmpty()) {
                    declarations.add(new Declaration(name, values.toArray(String[]::new)));
                    values.clear();
                }

                name = line.substring(0, line.length() - 2);
                continue;
            }

            if (!line.isBlank()) {
                values.add(line);
            }
        }

        if (!values.isEmpty()) {
            declarations.add(new Declaration(name, values.toArray(String[]::new)));
            values.clear();
        }

        try (PrintStream stream = new PrintStream(new FileOutputStream("glsl.txt"))) {
            StringBuilder builder = new StringBuilder();
            for (Declaration entry : declarations) {
                builder.append(entry.name()).append(" : ");

                String[] strings = entry.values;
                for (int i = 0; i < strings.length; i++) {
                    builder.append(strings[i]);
                    if (i < strings.length - 1) {
                        builder.append(" | ");
                    }
                }
                builder.append('\n');
            }
            stream.print(builder);
        }

        try (PrintStream stream = new PrintStream(new FileOutputStream("glsl_code.txt"))) {
            StringBuilder builder = new StringBuilder();
            for (Declaration entry : declarations) {
                StringBuilder methodName = new StringBuilder();
                char[] chars = entry.name.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                    if (i == 0) {
                        methodName.append(Character.toUpperCase(chars[i]));
                        continue;
                    }
                    if (chars[i] == '_') {
                        i++;
                        methodName.append(Character.toUpperCase(chars[i]));
                        continue;
                    }
                    methodName.append(chars[i]);
                }

                String behavior = Arrays.stream(entry.values).map(a -> "    // " + a).collect(Collectors.joining("\n"));
                builder.append("""
                        private static @Nullable GlslNode parse%s(GlslTokenReader reader) {
                        %s
                            return null; // TODO
                        }
                        
                        """.formatted(methodName, behavior));
            }
            stream.print(builder);
        }
    }

    private enum Grammar {
        CONST, BOOL, FLOAT, INT, UINT, DOUBLE,

        BVEC2, BVEC3, BVEC4, IVEC2, IVEC3, IVEC4, UVEC2, UVEC3, UVEC4, VEC2, VEC3, VEC4,

        MAT2, MAT3, MAT4, MAT2X2, MAT2X3, MAT2X4, MAT3X2, MAT3X3, MAT3X4, MAT4X2, MAT4X3, MAT4X4,

        DVEC2, DVEC3, DVEC4, DMAT2, DMAT3, DMAT4, DMAT2X2, DMAT2X3, DMAT2X4, DMAT3X2, DMAT3X3, DMAT3X4, DMAT4X2, DMAT4X3, DMAT4X4,

        CENTROID, IN, OUT, INOUT, UNIFORM, PATCH, SAMPLE, BUFFER, SHARED, COHERENT, VOLATILE, RESTRICT, READONLY, WRITEONLY, NOPERSPECTIVE, FLAT, SMOOTH, LAYOUT,

        ATOMIC_UINT,

        SAMPLER2D, SAMPLER3D, SAMPLERCUBE, SAMPLER2DSHADOW, SAMPLERCUBESHADOW, SAMPLER2DARRAY, SAMPLER2DARRAYSHADOW, ISAMPLER2D, ISAMPLER3D, ISAMPLERCUBE, ISAMPLER2DARRAY, USAMPLER2D, USAMPLER3D, USAMPLERCUBE, USAMPLER2DARRAY,

        SAMPLER1D, SAMPLER1DSHADOW, SAMPLER1DARRAY, SAMPLER1DARRAYSHADOW, ISAMPLER1D, ISAMPLER1DARRAY, USAMPLER1D, USAMPLER1DARRAY, SAMPLER2DRECT, SAMPLER2DRECTSHADOW, ISAMPLER2DRECT, USAMPLER2DRECT,

        SAMPLERBUFFER, ISAMPLERBUFFER, USAMPLERBUFFER, SAMPLERCUBEARRAY, SAMPLERCUBEARRAYSHADOW, ISAMPLERCUBEARRAY, USAMPLERCUBEARRAY, SAMPLER2DMS, ISAMPLER2DMS, USAMPLER2DMS, SAMPLER2DMSARRAY, ISAMPLER2DMSARRAY, USAMPLER2DMSARRAY, IMAGE2D, IIMAGE2D, UIMAGE2D, IMAGE3D, IIMAGE3D, UIMAGE3D, IMAGECUBE, IIMAGECUBE, UIMAGECUBE, IMAGEBUFFER, IIMAGEBUFFER, UIMAGEBUFFER, IMAGE2DARRAY, IIMAGE2DARRAY, UIMAGE2DARRAY, IMAGECUBEARRAY, IIMAGECUBEARRAY, UIMAGECUBEARRAY,

        IMAGE1D, IIMAGE1D, UIMAGE1D, IMAGE1DARRAY, IIMAGE1DARRAY, UIMAGE1DARRAY, IMAGE2DRECT, IIMAGE2DRECT, UIMAGE2DRECT, IMAGE2DMS, IIMAGE2DMS, UIMAGE2DMS, IMAGE2DMSARRAY, IIMAGE2DMSARRAY, UIMAGE2DMSARRAY,

        STRUCT, VOID,

        WHILE, BREAK, CONTINUE, DO, ELSE, FOR, IF, DISCARD, RETURN, SWITCH, CASE, DEFAULT, SUBROUTINE,

        IDENTIFIER, TYPE_NAME, FLOATCONSTANT, INTCONSTANT, UINTCONSTANT, BOOLCONSTANT, DOUBLECONSTANT, FIELD_SELECTION,

        LEFT_OP, RIGHT_OP, INC_OP, DEC_OP, LE_OP, GE_OP, EQ_OP, NE_OP, AND_OP, OR_OP, XOR_OP, MUL_ASSIGN, DIV_ASSIGN, ADD_ASSIGN, MOD_ASSIGN, LEFT_ASSIGN, RIGHT_ASSIGN, AND_ASSIGN, XOR_ASSIGN, OR_ASSIGN, SUB_ASSIGN, LEFT_PAREN, RIGHT_PAREN, LEFT_BRACKET, RIGHT_BRACKET, LEFT_BRACE, RIGHT_BRACE, DOT, COMMA, COLON, EQUAL, SEMICOLON, BANG, DASH, TILDE, PLUS, STAR, SLASH, PERCENT, LEFT_ANGLE, RIGHT_ANGLE, VERTICAL_BAR, CARET, AMPERSAND, QUESTION,

        INVARIANT, PRECISE, HIGH_PRECISION, MEDIUM_PRECISION, LOW_PRECISION, PRECISION;

        private static final Set<String> NAMES = Arrays.stream(values()).map(Grammar::name).collect(Collectors.toSet());
    }

    private record Declaration(String name, String... values) {
    }

    private interface GrammarElement {

        static GrammarElement token(Grammar grammar) {
            return new EnumGrammarElement(grammar);
        }

        static GrammarElement recursive(String name) {
            return new RecursiveGrammarElement(name);
        }

        static GrammarElement of(String name, GrammarElement... elements) {
            if (elements.length == 0) {
                throw new IllegalArgumentException("Grammar elements cannot be empty");
            }
            if (elements.length == 1) {
                return elements[0];
            }

            return new CompoundGrammarElement(name, elements);
        }

        record EnumGrammarElement(Grammar grammar) implements GrammarElement {
            @Override
            public String toString() {
                return this.grammar.toString();
            }
        }

        record RecursiveGrammarElement(String name) implements GrammarElement {
            @Override
            public String toString() {
                return this.name;
            }
        }

        record CompoundGrammarElement(String name, GrammarElement[] elements) implements GrammarElement {
            @Override
            public String toString() {
                return "CompoundGrammarElement[name=" + this.name + ", elements=" + Arrays.toString(this.elements) + ']';
            }
        }
    }
}