package foundry.veil.impl.glsl;

import foundry.veil.impl.glsl.grammar.*;
import foundry.veil.impl.glsl.node.*;
import foundry.veil.impl.glsl.node.branch.GlslCaseLabelNode;
import foundry.veil.impl.glsl.node.branch.GlslSelectionNode;
import foundry.veil.impl.glsl.node.branch.GlslSwitchNode;
import foundry.veil.impl.glsl.node.expression.*;
import foundry.veil.impl.glsl.node.function.GlslFunction;
import foundry.veil.impl.glsl.node.function.GlslFunctionHeader;
import foundry.veil.impl.glsl.node.function.GlslInvokeFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslArrayNode;
import foundry.veil.impl.glsl.node.primary.*;
import foundry.veil.impl.glsl.node.variable.GlslFieldNode;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.node.variable.GlslVariableNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class GlslParser {

    private static final String GRAMMAR = """
            variable_identifier : IDENTIFIER
            primary_expression : variable_identifier | INTCONSTANT | UINTCONSTANT | FLOATCONSTANT | BOOLCONSTANT | DOUBLECONSTANT | LEFT_PAREN expression RIGHT_PAREN
            postfix_expression : primary_expression | postfix_expression LEFT_BRACKET integer_expression RIGHT_BRACKET | function_call | postfix_expression DOT FIELD_SELECTION | postfix_expression INC_OP | postfix_expression DEC_OP
            integer_expression : expression
            function_call : function_call_or_method
            function_call_or_method : function_call_generic
            function_call_generic : function_call_header_with_parameters RIGHT_PAREN | function_call_header_no_parameters RIGHT_PAREN
            function_call_header_no_parameters : function_call_header VOID | function_call_header
            function_call_header_with_parameters : function_call_header assignment_expression | function_call_header_with_parameters COMMA assignment_expression
            function_call_header : function_identifier LEFT_PAREN
            function_identifier : type_specifier | postfix_expression
            unary_expression : postfix_expression | INC_OP unary_expression | DEC_OP unary_expression | unary_operator unary_expression
            unary_operator : PLUS | DASH | BANG | TILDE
            multiplicative_expression : unary_expression | multiplicative_expression STAR unary_expression | multiplicative_expression SLASH unary_expression | multiplicative_expression PERCENT unary_expression
            additive_expression : multiplicative_expression | additive_expression PLUS multiplicative_expression | additive_expression DASH multiplicative_expression
            shift_expression : additive_expression | shift_expression LEFT_OP additive_expression | shift_expression RIGHT_OP additive_expression
            relational_expression : shift_expression | relational_expression LEFT_ANGLE shift_expression | relational_expression RIGHT_ANGLE shift_expression | relational_expression LE_OP shift_expression | relational_expression GE_OP shift_expression
            equality_expression : relational_expression | equality_expression EQ_OP relational_expression | equality_expression NE_OP relational_expression
            and_expression : equality_expression | and_expression AMPERSAND equality_expression
            exclusive_or_expression : and_expression | exclusive_or_expression CARET and_expression
            inclusive_or_expression : exclusive_or_expression | inclusive_or_expression VERTICAL_BAR exclusive_or_expression
            logical_and_expression : inclusive_or_expression | logical_and_expression AND_OP inclusive_or_expression
            logical_xor_expression : logical_and_expression | logical_xor_expression XOR_OP logical_and_expression
            logical_or_expression : logical_xor_expression | logical_or_expression OR_OP logical_xor_expression
            conditional_expression : logical_or_expression | logical_or_expression QUESTION expression COLON assignment_expression
            assignment_expression : conditional_expression | unary_expression assignment_operator assignment_expression
            assignment_operator : EQUAL | MUL_ASSIGN | DIV_ASSIGN | MOD_ASSIGN | ADD_ASSIGN | SUB_ASSIGN | LEFT_ASSIGN | RIGHT_ASSIGN | AND_ASSIGN | XOR_ASSIGN | OR_ASSIGN
            expression : assignment_expression | expression COMMA assignment_expression
            constant_expression : conditional_expression
            declaration : function_prototype SEMICOLON | init_declarator_list SEMICOLON | PRECISION precision_qualifier type_specifier SEMICOLON | type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE SEMICOLON | type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE IDENTIFIER SEMICOLON | type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE IDENTIFIER array_specifier SEMICOLON | type_qualifier SEMICOLON | type_qualifier IDENTIFIER SEMICOLON | type_qualifier IDENTIFIER identifier_list SEMICOLON
            identifier_list : COMMA IDENTIFIER | identifier_list COMMA IDENTIFIER
            function_prototype : function_declarator RIGHT_PAREN
            function_declarator : function_header | function_header_with_parameters
            function_header_with_parameters : function_header parameter_declaration | function_header_with_parameters COMMA parameter_declaration
            function_header : fully_specified_type IDENTIFIER LEFT_PAREN
            parameter_declarator : type_specifier IDENTIFIER | type_specifier IDENTIFIER array_specifier
            parameter_declaration : type_qualifier parameter_declarator | parameter_declarator | type_qualifier parameter_type_specifier | parameter_type_specifier
            parameter_type_specifier : type_specifier
            init_declarator_list : single_declaration | init_declarator_list COMMA IDENTIFIER | init_declarator_list COMMA IDENTIFIER array_specifier | init_declarator_list COMMA IDENTIFIER array_specifier EQUAL initializer | init_declarator_list COMMA IDENTIFIER EQUAL initializer
            single_declaration : fully_specified_type | fully_specified_type IDENTIFIER | fully_specified_type IDENTIFIER array_specifier | fully_specified_type IDENTIFIER array_specifier EQUAL initializer | fully_specified_type IDENTIFIER EQUAL initializer
            fully_specified_type : type_specifier | type_qualifier type_specifier
            invariant_qualifier : INVARIANT
            interpolation_qualifier : SMOOTH | FLAT | NOPERSPECTIVE
            layout_qualifier : LAYOUT LEFT_PAREN layout_qualifier_id_list RIGHT_PAREN
            layout_qualifier_id_list : layout_qualifier_id | layout_qualifier_id_list COMMA layout_qualifier_id
            layout_qualifier_id : IDENTIFIER | IDENTIFIER EQUAL constant_expression | SHARED
            precise_qualifier : PRECISE
            type_qualifier : single_type_qualifier | type_qualifier single_type_qualifier
            single_type_qualifier : storage_qualifier | layout_qualifier | precision_qualifier | interpolation_qualifier | invariant_qualifier | precise_qualifier
            storage_qualifier : CONST | IN | OUT | INOUT | CENTROID | PATCH | SAMPLE | UNIFORM | BUFFER | SHARED | COHERENT | VOLATILE | RESTRICT | READONLY | WRITEONLY | SUBROUTINE | SUBROUTINE LEFT_PAREN type_name_list RIGHT_PAREN
            type_name_list : TYPE_NAME | type_name_list COMMA TYPE_NAME
            type_specifier : type_specifier_nonarray | type_specifier_nonarray array_specifier
            array_specifier : LEFT_BRACKET RIGHT_BRACKET | LEFT_BRACKET conditional_expression RIGHT_BRACKET | array_specifier LEFT_BRACKET RIGHT_BRACKET | array_specifier LEFT_BRACKET conditional_expression RIGHT_BRACKET
            type_specifier_nonarray : VOID | FLOAT | DOUBLE | INT | UINT | BOOL | VEC2 | VEC3 | VEC4 | DVEC2 | DVEC3 | DVEC4 | BVEC2 | BVEC3 | BVEC4 | IVEC2 | IVEC3 | IVEC4 | UVEC2 | UVEC3 | UVEC4 | MAT2 | MAT3 | MAT4 | MAT2X2 | MAT2X3 | MAT2X4 | MAT3X2 | MAT3X3 | MAT3X4 | MAT4X2 | MAT4X3 | MAT4X4 | DMAT2 | DMAT3 | DMAT4 | DMAT2X2 | DMAT2X3 | DMAT2X4 | DMAT3X2 | DMAT3X3 | DMAT3X4 | DMAT4X2 | DMAT4X3 | DMAT4X4 | ATOMIC_UINT | SAMPLER2D | SAMPLER3D | SAMPLERCUBE | SAMPLER2DSHADOW | SAMPLERCUBESHADOW | SAMPLER2DARRAY | SAMPLER2DARRAYSHADOW | SAMPLERCUBEARRAY | SAMPLERCUBEARRAYSHADOW | ISAMPLER2D | ISAMPLER3D | ISAMPLERCUBE | ISAMPLER2DARRAY | ISAMPLERCUBEARRAY | USAMPLER2D | USAMPLER3D | USAMPLERCUBE | USAMPLER2DARRAY | USAMPLERCUBEARRAY | SAMPLER1D | SAMPLER1DSHADOW | SAMPLER1DARRAY | SAMPLER1DARRAYSHADOW | ISAMPLER1D | ISAMPLER1DARRAY | USAMPLER1D | USAMPLER1DARRAY | SAMPLER2DRECT | SAMPLER2DRECTSHADOW | ISAMPLER2DRECT | USAMPLER2DRECT | SAMPLERBUFFER | ISAMPLERBUFFER | USAMPLERBUFFER | SAMPLER2DMS | ISAMPLER2DMS | USAMPLER2DMS | SAMPLER2DMSARRAY | ISAMPLER2DMSARRAY | USAMPLER2DMSARRAY | IMAGE2D | IIMAGE2D | UIMAGE2D | IMAGE3D | IIMAGE3D | UIMAGE3D | IMAGECUBE | IIMAGECUBE | UIMAGECUBE | IMAGEBUFFER | IIMAGEBUFFER | UIMAGEBUFFER | IMAGE1D | IIMAGE1D | UIMAGE1D | IMAGE1DARRAY | IIMAGE1DARRAY | UIMAGE1DARRAY | IMAGE2DRECT | IIMAGE2DRECT | UIMAGE2DRECT | IMAGE2DARRAY | IIMAGE2DARRAY | UIMAGE2DARRAY | IMAGECUBEARRAY | IIMAGECUBEARRAY | UIMAGECUBEARRAY | IMAGE2DMS | IIMAGE2DMS | UIMAGE2DMS | IMAGE2DMSARRAY | IIMAGE2DMSARRAY | UIMAGE2DMSARRAY | struct_specifier | TYPE_NAME
            precision_qualifier : HIGH_PRECISION | MEDIUM_PRECISION | LOW_PRECISION
            struct_specifier : STRUCT IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE | STRUCT LEFT_BRACE struct_declaration_list RIGHT_BRACE
            struct_declaration_list : struct_declaration | struct_declaration_list struct_declaration
            struct_declaration : type_specifier struct_declarator_list SEMICOLON | type_qualifier type_specifier struct_declarator_list SEMICOLON
            struct_declarator_list : struct_declarator | struct_declarator_list COMMA struct_declarator
            struct_declarator : IDENTIFIER | IDENTIFIER array_specifier
            initializer : assignment_expression | LEFT_BRACE initializer_list RIGHT_BRACE | LEFT_BRACE initializer_list COMMA RIGHT_BRACE
            initializer_list : initializer | initializer_list COMMA initializer
            declaration_statement : declaration
            statement : compound_statement | simple_statement
            simple_statement : declaration_statement | expression_statement | selection_statement | switch_statement | case_label | iteration_statement | jump_statement
            compound_statement : LEFT_BRACE RIGHT_BRACE | LEFT_BRACE statement_list RIGHT_BRACE
            statement_no_new_scope : compound_statement_no_new_scope | simple_statement
            compound_statement_no_new_scope : LEFT_BRACE RIGHT_BRACE | LEFT_BRACE statement_list RIGHT_BRACE
            statement_list : statement | statement_list statement
            expression_statement : SEMICOLON | expression SEMICOLON
            selection_statement : IF LEFT_PAREN expression RIGHT_PAREN selection_rest_statement
            selection_rest_statement : statement ELSE statement | statement
            condition : expression | fully_specified_type IDENTIFIER EQUAL initializer
            switch_statement : SWITCH LEFT_PAREN expression RIGHT_PAREN LEFT_BRACE switch_statement_list | RIGHT_BRACE
            switch_statement_list : statement_list
            case_label : CASE expression COLON | DEFAULT COLON
            iteration_statement : WHILE LEFT_PAREN condition RIGHT_PAREN statement_no_new_scope | DO statement WHILE LEFT_PAREN expression RIGHT_PAREN SEMICOLON | FOR LEFT_PAREN for_init_statement for_rest_statement RIGHT_PAREN statement_no_new_scope
            for_init_statement : expression_statement | declaration_statement
            conditionopt : condition
            for_rest_statement : conditionopt SEMICOLON | conditionopt SEMICOLON expression
            jump_statement : CONTINUE SEMICOLON | BREAK SEMICOLON | RETURN SEMICOLON | RETURN expression SEMICOLON | DISCARD SEMICOLON
            translation_unit : external_declaration | translation_unit external_declaration
            external_declaration : function_definition | declaration | SEMICOLON
            function_definition : function_prototype compound_statement_no_new_scope
            """;

    public static GlslTree parse(GlslLexer.Token[] tokens) throws GlslSyntaxException {
        GlslTokenReader reader = new GlslTokenReader(tokens);

        GlslVersion version = new GlslVersion(110, true);

        // Try to parse version statements
        GlslLexer.Token token = reader.peek();
        if (token.type() == GlslLexer.TokenType.DIRECTIVE && token.value().startsWith("#version ")) {
            reader.skip();
            String[] parts = token.value().substring(9).split(" +", 2);
            try {
                int ver = Integer.parseInt(parts[0]);
                boolean core = parts.length == 1 || parts[1].equals("core");
                version = new GlslVersion(ver, core);
            } catch (NumberFormatException e) {
                throw reader.error("Invalid Version: " + token.value() + ". " + e.getMessage());
            }
        }

        List<GlslNode> body = new ArrayList<>();
        while (reader.canRead()) {
            GlslNode functionDefinition = parseFunctionDefinition(reader);
            if (functionDefinition != null) {
                body.add(functionDefinition);
                continue;
            }

            GlslNode declaration = parseDeclaration(reader);
            if (declaration != null) {
                body.add(declaration);
                continue;
            }

            if (reader.tryConsume(GlslLexer.TokenType.SEMICOLON)) {
                continue;
            }

            throw reader.error("Failed");
        }

        return new GlslTree(version, body);
    }

    private static @Nullable GlslNode parsePrimaryExpression(GlslTokenReader reader) {
        // IDENTIFIER
        // INTCONSTANT
        // UINTCONSTANT
        // FLOATCONSTANT
        // BOOLCONSTANT
        // DOUBLECONSTANT
        // LEFT_PAREN condition RIGHT_PAREN

        if (reader.tryConsume(GlslLexer.TokenType.IDENTIFIER)) {
            String variableName = reader.peek(-1).value();
            return new GlslVariableNode(variableName);
        }
        if (reader.tryConsume(GlslLexer.TokenType.INTEGER_DECIMAL_CONSTANT)) {
            return new GlslIntConstantNode(GlslIntFormat.DECIMAL, Integer.parseUnsignedInt(reader.peek(-1).value(), 10));
        }
        if (reader.tryConsume(GlslLexer.TokenType.INTEGER_HEXADECIMAL_CONSTANT)) {
            return new GlslIntConstantNode(GlslIntFormat.HEXADECIMAL, Integer.parseUnsignedInt(reader.peek(-1).value(), 16));
        }
        if (reader.tryConsume(GlslLexer.TokenType.INTEGER_OCTAL_CONSTANT)) {
            return new GlslIntConstantNode(GlslIntFormat.OCTAL, Integer.parseUnsignedInt(reader.peek(-1).value(), 8));
        }
        if (reader.tryConsume(GlslLexer.TokenType.UINTEGER_DECIMAL_CONSTANT)) {
            return new GlslUIntConstantNode(GlslIntFormat.DECIMAL, Integer.parseUnsignedInt(reader.peek(-1).value(), 10));
        }
        if (reader.tryConsume(GlslLexer.TokenType.UINTEGER_HEXADECIMAL_CONSTANT)) {
            return new GlslUIntConstantNode(GlslIntFormat.HEXADECIMAL, Integer.parseUnsignedInt(reader.peek(-1).value(), 16));
        }
        if (reader.tryConsume(GlslLexer.TokenType.UINTEGER_OCTAL_CONSTANT)) {
            return new GlslUIntConstantNode(GlslIntFormat.OCTAL, Integer.parseUnsignedInt(reader.peek(-1).value(), 8));
        }
        if (reader.tryConsume(GlslLexer.TokenType.FLOATING_CONSTANT)) {
            return new GlslFloatConstantNode(Float.parseFloat(reader.peek(-1).value()));
        }
        if (reader.tryConsume(GlslLexer.TokenType.BOOL_CONSTANT)) {
            return new GlslBoolConstantNode(Boolean.parseBoolean(reader.peek(-1).value()));
        }

        int cursor = reader.getCursor();
        if (reader.tryConsume(GlslLexer.TokenType.LEFT_PAREN)) {
            GlslNode condition = parseCondition(reader);
            if (condition != null && reader.tryConsume(GlslLexer.TokenType.RIGHT_PAREN)) {
                return condition;
            }
        }

        reader.setCursor(cursor);
        return null;
    }

    private static @Nullable GlslNode parsePostfixExpression(boolean allowFunction, GlslTokenReader reader) {
        int cursor = reader.getCursor();

//        primary_expression
//        primary_expression LEFT_BRACKET integer_expression RIGHT_BRACKET
//        primary_expression DOT FIELD_SELECTION
//        primary_expression INC_OP
//        primary_expression DEC_OP
//        function_call LEFT_BRACKET integer_expression RIGHT_BRACKET
//        function_call
//        function_call DOT FIELD_SELECTION
//        function_call INC_OP
//        function_call DEC_OP

        if (allowFunction) {
            // function_call
            GlslNode functionCall = parseFunctionCallGeneric(reader);
            if (functionCall != null) {
                // function_call LEFT_BRACKET integer_expression RIGHT_BRACKET
                int functionCursor = reader.getCursor();
                if (reader.tryConsume(GlslLexer.TokenType.LEFT_BRACKET)) {
                    GlslNode integerExpression = parseIntegerExpression(reader);
                    if (integerExpression != null && reader.tryConsume(GlslLexer.TokenType.RIGHT_BRACKET)) {
                        return new GlslArrayNode(functionCall, integerExpression);
                    }
                }
                reader.setCursor(functionCursor);

                // function_call DOT FIELD_SELECTION
                if (reader.tryConsume(GlslLexer.TokenType.DOT, GlslLexer.TokenType.IDENTIFIER)) {
                    return new GlslFieldNode(functionCall, reader.peek(-1).value());
                }
                reader.setCursor(functionCursor);

                // function_call INC_OP
                if (reader.tryConsume(GlslLexer.TokenType.INC_OP)) {
                    return new GlslUnaryNode(functionCall, GlslUnaryNode.Operand.POST_INCREMENT);
                }
                reader.setCursor(functionCursor);

                // function_call DEC_OP
                if (reader.tryConsume(GlslLexer.TokenType.DEC_OP)) {
                    return new GlslUnaryNode(functionCall, GlslUnaryNode.Operand.POST_DECREMENT);
                }
                reader.setCursor(functionCursor);

                return functionCall;
            }
            reader.setCursor(cursor);
        }

        // primary_expression
        GlslNode primaryExpression = parsePrimaryExpression(reader);
        if (primaryExpression != null) {
            // primary_expression LEFT_BRACKET integer_expression RIGHT_BRACKET
            int expressionCursor = reader.getCursor();
            if (reader.tryConsume(GlslLexer.TokenType.LEFT_BRACKET)) {
                GlslNode integerExpression = parseIntegerExpression(reader);
                if (integerExpression != null && reader.tryConsume(GlslLexer.TokenType.RIGHT_BRACKET)) {
                    return new GlslArrayNode(primaryExpression, integerExpression);
                }
            }
            reader.setCursor(expressionCursor);

            // primary_expression DOT FIELD_SELECTION
            if (reader.tryConsume(GlslLexer.TokenType.DOT, GlslLexer.TokenType.IDENTIFIER)) {
                return new GlslFieldNode(primaryExpression, reader.peek(-1).value());
            }
            reader.setCursor(expressionCursor);
            // primary_expression INC_OP
            if (reader.tryConsume(GlslLexer.TokenType.INC_OP)) {
                return new GlslUnaryNode(primaryExpression, GlslUnaryNode.Operand.POST_INCREMENT);
            }
            reader.setCursor(expressionCursor);

            // primary_expression DEC_OP
            if (reader.tryConsume(GlslLexer.TokenType.DEC_OP)) {
                return new GlslUnaryNode(primaryExpression, GlslUnaryNode.Operand.POST_DECREMENT);
            }
            reader.setCursor(expressionCursor);

            return primaryExpression;
        }

        reader.setCursor(cursor);
        return null;
    }

    private static @Nullable GlslNode parseIntegerExpression(GlslTokenReader reader) {
        return parseCondition(reader);
    }

    private static @Nullable GlslNode parseFunctionCallGeneric(GlslTokenReader reader) {
        GlslNode functionCallHeader = parseFunctionCallHeader(reader);
        if (functionCallHeader == null) {
            return null;
        }

        int cursor = reader.getCursor();

        // function_call_header assignment_expression RIGHT_PAREN
        // function_call_header_with_parameters COMMA assignment_expression RIGHT_PAREN
        int parameterCursor = reader.getCursor();
        List<GlslNode> parameters = new ArrayList<>();
        while (reader.canRead()) {
            GlslNode parameter = parseAssignmentExpression(reader);
            if (parameter == null) {
                reader.setCursor(parameterCursor);
                break;
            }

            parameters.add(parameter);
            parameterCursor = reader.getCursor();

            if (!reader.tryConsume(GlslLexer.TokenType.COMMA)) {
                break;
            }
        }

        if (reader.tryConsume(GlslLexer.TokenType.RIGHT_PAREN)) {
            return new GlslInvokeFunctionNode(functionCallHeader, parameters);
        }
        reader.setCursor(cursor);

        // function_call_header VOID RIGHT_PAREN
        if (reader.tryConsume(GlslLexer.TokenType.VOID, GlslLexer.TokenType.RIGHT_PAREN)) {
            return new GlslInvokeFunctionNode(functionCallHeader, Collections.emptyList());
        }
        reader.setCursor(cursor);

        // function_call_header RIGHT_PAREN
        if (reader.tryConsume(GlslLexer.TokenType.RIGHT_PAREN)) {
            return new GlslInvokeFunctionNode(functionCallHeader, Collections.emptyList());
        }

        reader.setCursor(cursor);
        return null;
    }

    private static @Nullable GlslNode parseFunctionCallHeaderNoParameters(GlslTokenReader reader) {
        // function_call_header VOID
        // function_call_header
        return null; // TODO
    }

    private static @Nullable GlslNode parseFunctionCallHeaderWithParameters(GlslTokenReader reader) {
        // function_call_header assignment_expression
        // function_call_header_with_parameters COMMA assignment_expression
        return null; // TODO
    }

    private static @Nullable GlslNode parseFunctionCallHeader(GlslTokenReader reader) {
        // FIXME
        // type_specifier LEFT_PAREN
        // postfix_expression LEFT_PAREN

        int cursor = reader.getCursor();
        GlslNode expression = parsePostfixExpression(false, reader);
        if (expression != null && reader.tryConsume(GlslLexer.TokenType.LEFT_PAREN)) {
            return expression;
        }
        reader.setCursor(cursor);
        return null;
    }

    private static @Nullable GlslNode parseUnaryExpression(GlslTokenReader reader) {
        // unary_operator unary_expression

        GlslNode expression = parsePostfixExpression(true, reader);
        if (expression != null) {
            return expression;
        }

        int cursor = reader.getCursor();

        // INC_OP unary_expression
        if (reader.tryConsume(GlslLexer.TokenType.INC_OP)) {
            GlslNode right = parseUnaryExpression(reader);
            if (right != null) {
                return new GlslUnaryNode(right, GlslUnaryNode.Operand.PRE_INCREMENT);
            }
        }
        reader.setCursor(cursor);

        // DEC_OP unary_expression
        if (reader.tryConsume(GlslLexer.TokenType.DEC_OP)) {
            GlslNode right = parseUnaryExpression(reader);
            if (right != null) {
                return new GlslUnaryNode(right, GlslUnaryNode.Operand.PRE_DECREMENT);
            }
        }
        reader.setCursor(cursor);

        // postfix_expression
        if (reader.canRead()) {
            GlslUnaryNode.Operand operator = reader.peek().type().asUnaryOperator();
            if (operator != null) {
                GlslNode right = parseUnaryExpression(reader);
                if (right != null) {
                    return new GlslUnaryNode(right, operator);
                }
            }
        }

        reader.setCursor(cursor);
        return null;
    }

    private static @Nullable GlslNode parseSimpleExpression(GlslTokenReader reader, Function<GlslTokenReader, GlslNode> parser, GlslLexer.TokenType operator, Function<List<GlslNode>, GlslNode> join) {
        int cursor = reader.getCursor();
        List<GlslNode> expressions = new ArrayList<>();
        while (reader.canRead()) {
            GlslNode expression = parser.apply(reader);
            if (expression == null) {
                reader.setCursor(cursor);
                break;
            }

            expressions.add(expression);
            cursor = reader.getCursor();

            if (!reader.tryConsume(operator)) {
                break;
            }
        }

        if (expressions.isEmpty()) {
            return null;
        }
        if (expressions.size() == 1) {
            return expressions.get(0);
        }
        return join.apply(expressions);
    }

    private static @Nullable GlslNode parseMultiplicativeExpression(GlslTokenReader reader) {
        // unary_expression
        // multiplicative_expression STAR unary_expression
        // multiplicative_expression SLASH unary_expression
        // multiplicative_expression PERCENT unary_expression

        GlslNode left = parseUnaryExpression(reader);
        if (left == null) {
            return null;
        }

        while (reader.canRead()) {
            int cursor = reader.getCursor();
            if (reader.tryConsume(GlslLexer.TokenType.STAR)) {
                GlslNode right = parseUnaryExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslOperationNode(left, right, GlslOperationNode.Operand.MULTIPLY);
                continue;
            }
            if (reader.tryConsume(GlslLexer.TokenType.SLASH)) {
                GlslNode right = parseUnaryExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslOperationNode(left, right, GlslOperationNode.Operand.DIVIDE);
                continue;
            }
            if (reader.tryConsume(GlslLexer.TokenType.PERCENT)) {
                GlslNode right = parseUnaryExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslOperationNode(left, right, GlslOperationNode.Operand.MODULO);
                continue;
            }
            break;
        }

        return left;
    }

    private static @Nullable GlslNode parseAdditiveExpression(GlslTokenReader reader) {
        // multiplicative_expression
        // additive_expression PLUS multiplicative_expression
        // additive_expression DASH multiplicative_expression

        GlslNode left = parseMultiplicativeExpression(reader);
        if (left == null) {
            return null;
        }

        while (reader.canRead()) {
            int cursor = reader.getCursor();
            if (reader.tryConsume(GlslLexer.TokenType.PLUS)) {
                GlslNode right = parseMultiplicativeExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslOperationNode(left, right, GlslOperationNode.Operand.ADD);
                continue;
            }
            if (reader.tryConsume(GlslLexer.TokenType.DASH)) {
                GlslNode right = parseMultiplicativeExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslOperationNode(left, right, GlslOperationNode.Operand.SUBTRACT);
                continue;
            }
            break;
        }

        return left;
    }

    private static @Nullable GlslNode parseShiftExpression(GlslTokenReader reader) {
        // additive_expression
        // shift_expression LEFT_OP additive_expression
        // shift_expression RIGHT_OP additive_expression

        GlslNode left = parseAdditiveExpression(reader);
        if (left == null) {
            return null;
        }

        while (reader.canRead()) {
            int cursor = reader.getCursor();
            if (reader.tryConsume(GlslLexer.TokenType.LEFT_OP)) {
                GlslNode right = parseAdditiveExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslOperationNode(left, right, GlslOperationNode.Operand.LEFT_SHIFT);
                continue;
            }
            if (reader.tryConsume(GlslLexer.TokenType.RIGHT_OP)) {
                GlslNode right = parseAdditiveExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslOperationNode(left, right, GlslOperationNode.Operand.RIGHT_SHIFT);
                continue;
            }
            break;
        }

        return left;
    }

    private static @Nullable GlslNode parseRelationalExpression(GlslTokenReader reader) {
        // shift_expression
        // relational_expression LEFT_ANGLE shift_expression
        // relational_expression RIGHT_ANGLE shift_expression
        // relational_expression LE_OP shift_expression
        // relational_expression GE_OP shift_expression

        GlslNode left = parseShiftExpression(reader);
        if (left == null) {
            return null;
        }

        while (reader.canRead()) {
            int cursor = reader.getCursor();
            if (reader.tryConsume(GlslLexer.TokenType.LEFT_ANGLE)) {
                GlslNode right = parseShiftExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslCompareNode(left, right, GlslCompareNode.Type.LESS);
                continue;
            }
            if (reader.tryConsume(GlslLexer.TokenType.RIGHT_ANGLE)) {
                GlslNode right = parseShiftExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslCompareNode(left, right, GlslCompareNode.Type.GREATER);
                continue;
            }
            if (reader.tryConsume(GlslLexer.TokenType.LE_OP)) {
                GlslNode right = parseShiftExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslCompareNode(left, right, GlslCompareNode.Type.LEQUAL);
                continue;
            }
            if (reader.tryConsume(GlslLexer.TokenType.GE_OP)) {
                GlslNode right = parseShiftExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslCompareNode(left, right, GlslCompareNode.Type.GEQUAL);
                continue;
            }
            break;
        }

        return left;
    }

    private static @Nullable GlslNode parseEqualityExpression(GlslTokenReader reader) {
        // relational_expression
        // equality_expression EQ_OP relational_expression
        // equality_expression NE_OP relational_expression

        GlslNode left = parseRelationalExpression(reader);
        if (left == null) {
            return null;
        }

        while (reader.canRead()) {
            int cursor = reader.getCursor();
            if (reader.tryConsume(GlslLexer.TokenType.EQ_OP)) {
                GlslNode right = parseRelationalExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslCompareNode(left, right, GlslCompareNode.Type.EQUAL);
                continue;
            }
            if (reader.tryConsume(GlslLexer.TokenType.NE_OP)) {
                GlslNode right = parseRelationalExpression(reader);
                if (right == null) {
                    reader.setCursor(cursor);
                    return left;
                }
                left = new GlslCompareNode(left, right, GlslCompareNode.Type.NOT_EQUAL);
                continue;
            }
            break;
        }

        return left;
    }

    private static @Nullable GlslNode parseAndExpression(GlslTokenReader reader) {
        // equality_expression
        // and_expression AMPERSAND equality_expression
        return parseSimpleExpression(reader, GlslParser::parseEqualityExpression, GlslLexer.TokenType.AMPERSAND, GlslAndNode::new);
    }

    private static @Nullable GlslNode parseExclusiveOrExpression(GlslTokenReader reader) {
        // and_expression
        // exclusive_or_expression CARET and_expression
        return parseSimpleExpression(reader, GlslParser::parseAndExpression, GlslLexer.TokenType.CARET, GlslExclusiveOrNode::new);
    }

    private static @Nullable GlslNode parseInclusiveOrExpression(GlslTokenReader reader) {
        // exclusive_or_expression
        // inclusive_or_expression VERTICAL_BAR exclusive_or_expression
        return parseSimpleExpression(reader, GlslParser::parseExclusiveOrExpression, GlslLexer.TokenType.VERTICAL_BAR, GlslLogicalAndNode::new);
    }

    private static @Nullable GlslNode parseLogicalAndExpression(GlslTokenReader reader) {
        // inclusive_or_expression
        // logical_and_expression AND_OP inclusive_or_expression
        return parseSimpleExpression(reader, GlslParser::parseInclusiveOrExpression, GlslLexer.TokenType.AND_OP, GlslLogicalAndNode::new);
    }

    private static @Nullable GlslNode parseLogicalXorExpression(GlslTokenReader reader) {
        // logical_and_expression
        // logical_xor_expression XOR_OP logical_and_expression
        return parseSimpleExpression(reader, GlslParser::parseLogicalAndExpression, GlslLexer.TokenType.XOR_OP, GlslLogicalXorNode::new);
    }

    private static @Nullable GlslNode parseLogicalOrExpression(GlslTokenReader reader) {
        // logical_xor_expression
        // logical_or_expression OR_OP logical_xor_expression
        return parseSimpleExpression(reader, GlslParser::parseLogicalXorExpression, GlslLexer.TokenType.OR_OP, GlslLogicalOrNode::new);
    }

    private static @Nullable GlslNode parseConditionalExpression(GlslTokenReader reader) {
        // logical_or_expression
        GlslNode logicalOr = parseLogicalOrExpression(reader);
        if (logicalOr == null) {
            return null;
        }

        // logical_or_expression QUESTION condition COLON assignment_expression
        int cursor = reader.getCursor();
        if (reader.tryConsume(GlslLexer.TokenType.QUESTION)) {
            GlslNode first = parseExpression(reader);
            if (first != null && reader.tryConsume(GlslLexer.TokenType.COLON)) {
                GlslNode branch = parseAssignmentExpression(reader);
                return new GlslConditionalNode(logicalOr, first, branch);
            }
        }

        reader.setCursor(cursor);
        return logicalOr;
    }

    private static @Nullable GlslNode parseAssignmentExpression(GlslTokenReader reader) {
        // unary_expression assignment_operator assignment_expression
        int cursor = reader.getCursor();
        GlslNode unaryExpression = parseUnaryExpression(reader);
        if (reader.canRead()) {
            GlslAssignmentNode.Operand assignmentOperator = reader.peek().type().asAssignmentOperator();
            if (assignmentOperator != null) {
                reader.skip();
                GlslNode right = parseAssignmentExpression(reader);
                return new GlslAssignmentNode(unaryExpression, right, assignmentOperator);
            }
        }
        reader.setCursor(cursor);

        // conditional_expression
        return parseConditionalExpression(reader);
    }

    private static @Nullable GlslNode parseExpression(GlslTokenReader reader) {
        int cursor = reader.getCursor();

        // assignment_expression
        // expression COMMA assignment_expression
        List<GlslNode> expressions = new ArrayList<>();
        while (reader.canRead()) {
            GlslNode expression = parseAssignmentExpression(reader);
            if (expression == null) {
                reader.setCursor(cursor);
                break;
            }

            expressions.add(expression);
            cursor = reader.getCursor();

            if (!reader.tryConsume(GlslLexer.TokenType.COMMA)) {
                break;
            }
        }

        if (expressions.isEmpty()) {
            return null;
        }

        return GlslNode.compound(expressions);
    }

    private static @Nullable GlslNode parseDeclaration(GlslTokenReader reader) {
        // function_prototype SEMICOLON
        // init_declarator_list SEMICOLON
        // PRECISION precision_qualifier type_specifier SEMICOLON
        // type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE SEMICOLON
        // type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE IDENTIFIER SEMICOLON
        // type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE IDENTIFIER array_specifier SEMICOLON
        // type_qualifier SEMICOLON
        // type_qualifier IDENTIFIER SEMICOLON
        // type_qualifier IDENTIFIER identifier_list SEMICOLON

        int cursor = reader.getCursor();

        // function_prototype SEMICOLON
        GlslNode functionPrototype = parseFunctionPrototype(reader);
        if (functionPrototype != null) {
            if (reader.tryConsume(GlslLexer.TokenType.SEMICOLON)) {
                return functionPrototype;
            }
            reader.setCursor(cursor);
        }

        // init_declarator_list SEMICOLON
        GlslNode initDeclaratorList = parseInitDeclaratorList(reader);
        if (initDeclaratorList != null) {
            if (reader.tryConsume(GlslLexer.TokenType.SEMICOLON)) {
                return initDeclaratorList;
            }
            reader.setCursor(cursor);
        }

        // PRECISION precision_qualifier type_specifier SEMICOLON
        if (reader.tryConsume(GlslLexer.TokenType.PRECISION)) {
            GlslTypeQualifier.Precision precisionQualifier = parsePrecisionQualifier(reader);
            if (precisionQualifier != null) {
                GlslTypeSpecifier typeSpecifier = parseTypeSpecifier(reader);
                if (typeSpecifier != null && reader.tryConsume(GlslLexer.TokenType.SEMICOLON)) {
                    return new PrecisionNode(precisionQualifier, typeSpecifier);
                }
            }
            reader.setCursor(cursor);
        }

        List<GlslTypeQualifier> typeQualifier = parseTypeQualifiers(reader);
        if (typeQualifier == null) {
            return null;
        }

        // FIXME
        cursor = reader.getCursor();
        if (reader.tryConsume(GlslLexer.TokenType.IDENTIFIER, GlslLexer.TokenType.LEFT_BRACE)) {
            String identifier = reader.peek(-2).value();

            GlslStructSpecifier structDeclaration = parseStructDeclaration(reader);
            if (structDeclaration != null && reader.tryConsume(GlslLexer.TokenType.RIGHT_BRACE)) {
                // type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE SEMICOLON
                if (reader.tryConsume(GlslLexer.TokenType.SEMICOLON)) {
                    // SUCCESS
                }

                if (reader.tryConsume(GlslLexer.TokenType.IDENTIFIER)) {
                    String label = reader.peek(-1).value();

                    // type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE IDENTIFIER SEMICOLON
                    if (reader.tryConsume(GlslLexer.TokenType.SEMICOLON)) {
                        // SUCCESS
                    }

                    // type_qualifier IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE IDENTIFIER array_specifier SEMICOLON
                    GlslTypeSpecifier arraySpecifier = parseArraySpecifier(reader, GlslTypeSpecifier.struct(structDeclaration));
                    if (arraySpecifier != null && reader.tryConsume(GlslLexer.TokenType.SEMICOLON)) {
                        // SUCCESS
                    }
                }
            }
        }
        reader.setCursor(cursor);

        // type_qualifier SEMICOLON
       if(reader.tryConsume(GlslLexer.TokenType.SEMICOLON)) {
           // FIXME struct declaration
       }

        // type_qualifier IDENTIFIER SEMICOLON
        // type_qualifier IDENTIFIER identifier_list SEMICOLON

        return null; // FIXME
    }

    private static @Nullable GlslNode parseIdentifierList(GlslTokenReader reader) {
        // COMMA IDENTIFIER
        // identifier_list COMMA IDENTIFIER
        return null; // TODO
    }

    private static @Nullable GlslFunctionHeader parseFunctionPrototype(GlslTokenReader reader) {
        int cursor = reader.getCursor();

        // fully_specified_type IDENTIFIER LEFT_PAREN RIGHT_PAREN
        GlslSpecifiedType fullySpecifiedType = parseFullySpecifiedType(reader);
        if (fullySpecifiedType != null && reader.tryConsume(GlslLexer.TokenType.IDENTIFIER, GlslLexer.TokenType.LEFT_PAREN, GlslLexer.TokenType.RIGHT_PAREN)) {
            String name = reader.peek(-3).value();
            return new GlslFunctionHeader(name, fullySpecifiedType, new ArrayList<>());
        }
        reader.setCursor(cursor);

        // function_header_with_parameters RIGHT_PAREN
        GlslFunctionHeader functionHeaderWithParameters = parseFunctionHeaderWithParameters(reader);
        if (functionHeaderWithParameters != null) {
            return functionHeaderWithParameters;
        }

        // fully_specified_type IDENTIFIER LEFT_PAREN parameter_declaration
        fullySpecifiedType = parseFullySpecifiedType(reader);
        if (fullySpecifiedType != null && reader.tryConsume(GlslLexer.TokenType.IDENTIFIER, GlslLexer.TokenType.LEFT_PAREN)) {
            String name = reader.peek(-2).value();
            GlslParameterDeclaration parameterDeclaration = parseParameterDeclaration(reader);
            if (parameterDeclaration != null && reader.tryConsume(GlslLexer.TokenType.RIGHT_PAREN)) {
                return new GlslFunctionHeader(name, fullySpecifiedType, Collections.singletonList(parameterDeclaration));
            }
        }
        reader.setCursor(cursor);

        // function_header_with_parameters COMMA parameter_declaration
        functionHeaderWithParameters = parseFunctionHeaderWithParameters(reader);
        if (functionHeaderWithParameters != null && reader.tryConsume(GlslLexer.TokenType.COMMA)) {
            GlslParameterDeclaration parameterDeclaration = parseParameterDeclaration(reader);
            if (parameterDeclaration != null && reader.tryConsume(GlslLexer.TokenType.RIGHT_PAREN)) {
                List<GlslParameterDeclaration> parameters = functionHeaderWithParameters.getParameters();
                parameters.add(parameterDeclaration);
                return functionHeaderWithParameters;
            }
        }
        reader.setCursor(cursor);
        return null;
    }

//    private static @Nullable GlslNode parseFunctionDeclarator(GlslTokenReader reader) {
//        // function_header
//        // function_header_with_parameters
//        return null; // TODO
//    }

    private static List<GlslParameterDeclaration> parseParameterList(GlslTokenReader reader) {
        List<GlslParameterDeclaration> parameters = new ArrayList<>();
        int cursor = reader.getCursor();
        while (reader.canRead()) {
            GlslParameterDeclaration parameterDeclaration = parseParameterDeclaration(reader);
            if (parameterDeclaration == null) {
                reader.setCursor(cursor);
                break;
            }

            cursor = reader.getCursor();
            if (!reader.tryConsume(GlslLexer.TokenType.COMMA)) {
                break;
            }
        }

        return parameters;
    }

    private static @Nullable GlslFunctionHeader parseFunctionHeaderWithParameters(GlslTokenReader reader) {
        // fully_specified_type IDENTIFIER LEFT_PAREN parameter_declaration
        // function_header_with_parameters COMMA parameter_declaration

        int cursor = reader.getCursor();
        GlslSpecifiedType fullySpecifiedType = parseFullySpecifiedType(reader);
        if (fullySpecifiedType != null && reader.tryConsume(GlslLexer.TokenType.IDENTIFIER, GlslLexer.TokenType.LEFT_PAREN)) {
            String name = reader.peek(-2).value();
            List<GlslParameterDeclaration> parameters = parseParameterList(reader);
            if (reader.tryConsume(GlslLexer.TokenType.LEFT_PAREN)) {
                return new GlslFunctionHeader(name, fullySpecifiedType, parameters);
            }
        }

        reader.setCursor(cursor);
        return null;
    }

    private static @Nullable GlslParameterDeclaration parseParameterDeclarator(GlslTokenReader reader) {
        // type_specifier IDENTIFIER
        // type_specifier IDENTIFIER array_specifier

        int cursor = reader.getCursor();
        GlslTypeSpecifier typeSpecifier = parseTypeSpecifier(reader);
        if (typeSpecifier == null) {
            return null;
        }

        if (!reader.tryConsume(GlslLexer.TokenType.IDENTIFIER)) {
            reader.setCursor(cursor);
            return null;
        }

        String name = reader.peek(-1).value();
        GlslTypeSpecifier arraySpecifier = parseArraySpecifier(reader, typeSpecifier);
        return new GlslParameterDeclaration(name, new GlslSpecifiedType(Objects.requireNonNullElse(arraySpecifier, typeSpecifier)));

    }

    private static @Nullable GlslParameterDeclaration parseParameterDeclaration(GlslTokenReader reader) {
        int cursor = reader.getCursor();

        // type_qualifier
        List<GlslTypeQualifier> typeQualifiers = parseTypeQualifiers(reader);
        if (typeQualifiers != null) {
            // type_qualifier parameter_declarator
            GlslParameterDeclaration parameterDeclarator = parseParameterDeclarator(reader);
            if (parameterDeclarator != null) {
                return parameterDeclarator.setQualifiers(typeQualifiers);
            }

            // type_qualifier parameter_type_specifier
            GlslTypeSpecifier parameterTypeSpecifier = parseTypeSpecifier(reader);
            if (parameterTypeSpecifier != null) {
                return new GlslParameterDeclaration(null, new GlslSpecifiedType(parameterTypeSpecifier, typeQualifiers));
            }
        }
        reader.setCursor(cursor);

        // parameter_declarator
        GlslParameterDeclaration parameterDeclarator = parseParameterDeclarator(reader);
        if (parameterDeclarator != null) {
            return parameterDeclarator;
        }

        // parameter_type_specifier
        GlslTypeSpecifier parameterTypeSpecifier = parseTypeSpecifier(reader);
        if (parameterTypeSpecifier != null) {
            return new GlslParameterDeclaration(null, new GlslSpecifiedType(parameterTypeSpecifier));
        }

        return null;
    }

    private static @Nullable GlslNode parseInitDeclaratorList(GlslTokenReader reader) {
        // single_declaration
        // init_declarator_list COMMA IDENTIFIER
        // init_declarator_list COMMA IDENTIFIER array_specifier
        // init_declarator_list COMMA IDENTIFIER array_specifier EQUAL initializer
        // init_declarator_list COMMA IDENTIFIER EQUAL initializer

        List<GlslNode> initDeclaratorList = new ArrayList<>();
        while (reader.canRead()) {
            GlslNode singleDeclaration = parseSingleDeclaration(reader);
            if (singleDeclaration == null) {
                break;
            }

            initDeclaratorList.add(singleDeclaration);
        }

        if (initDeclaratorList.isEmpty()) {
            return null;
        }
        return GlslNode.compound(initDeclaratorList);
    }

    private static @Nullable GlslNode parseSingleDeclaration(GlslTokenReader reader) {
        // fully_specified_type
        // fully_specified_type IDENTIFIER
        // fully_specified_type IDENTIFIER array_specifier
        // fully_specified_type IDENTIFIER array_specifier EQUAL initializer
        // fully_specified_type IDENTIFIER EQUAL initializer

        int cursor = reader.getCursor();
        GlslSpecifiedType fullySpecifiedType = parseFullySpecifiedType(reader);
        if (fullySpecifiedType == null) {
            return null; // TODO
        }

        if (!reader.tryConsume(GlslLexer.TokenType.IDENTIFIER)) {
            reader.setCursor(cursor);
            return null; // TODO
        }

        cursor = reader.getCursor();
        String name = reader.peek(-1).value();

        GlslSpecifiedType arraySpecifier = parseArraySpecifier(reader, fullySpecifiedType);
        if (arraySpecifier != null) {
            if (!reader.tryConsume(GlslLexer.TokenType.EQUAL)) {
                // fully_specified_type IDENTIFIER array_specifier
                return new GlslNewNode(arraySpecifier, name, null);
            }

            GlslNode initializer = parseInitializer(reader);
            if (initializer != null) {
                // fully_specified_type IDENTIFIER array_specifier EQUAL initializer
                return new GlslNewNode(arraySpecifier, name, initializer);
            }
        }
        reader.setCursor(cursor);

        if (reader.tryConsume(GlslLexer.TokenType.EQUAL)) {
            GlslNode initializer = parseInitializer(reader);
            if (initializer != null) {
                // fully_specified_type IDENTIFIER EQUAL initializer
                return new GlslNewNode(fullySpecifiedType, name, initializer);
            }
        }

        // fully_specified_type IDENTIFIER
        reader.setCursor(cursor);
        return new GlslNewNode(fullySpecifiedType, name, null);
    }

    private static @Nullable GlslSpecifiedType parseFullySpecifiedType(GlslTokenReader reader) {
        // type_specifier
        // type_qualifier type_specifier

        // type_specifier
        GlslTypeSpecifier typeSpecifier = parseTypeSpecifier(reader);
        if (typeSpecifier != null) {
            return new GlslSpecifiedType(typeSpecifier);
        }

        // type_qualifier type_specifier
        int cursor = reader.getCursor();
        List<GlslTypeQualifier> typeQualifiers = parseTypeQualifiers(reader);
        if (typeQualifiers != null) {
            typeSpecifier = parseTypeSpecifier(reader);
            if (typeSpecifier != null) {
                return new GlslSpecifiedType(typeSpecifier, typeQualifiers);
            }
        }

        reader.setCursor(cursor);
        return null;
    }

    private static @Nullable GlslNode parseInvariantQualifier(GlslTokenReader reader) {
        // INVARIANT
        return null; // TODO
    }

    private static @Nullable GlslTypeQualifier.Interpolation parseInterpolationQualifier(GlslTokenReader reader) {
        // SMOOTH
        // FLAT
        // NOPERSPECTIVE

        GlslLexer.TokenType type = reader.peekType(0);
        GlslTypeQualifier.Interpolation interpolationQualifier = type != null ? type.asInterpolationQualifier() : null;
        if (interpolationQualifier != null) {
            reader.skip();
            return interpolationQualifier;
        }
        return null;
    }

    private static @Nullable GlslTypeQualifier parseLayoutQualifier(GlslTokenReader reader) {
        // LAYOUT LEFT_PAREN layout_qualifier_id_list RIGHT_PAREN
        if (!reader.tryConsume(GlslLexer.TokenType.LAYOUT, GlslLexer.TokenType.LEFT_PAREN)) {
            return null;
        }

        // layout_qualifier_id_list
        int layoutCursor = reader.getCursor();
        List<GlslTypeQualifier.LayoutId> layoutQualifierIds = new ArrayList<>();
        while (reader.canRead()) {
            GlslTypeQualifier.LayoutId qualifier = null;
            if (reader.tryConsume(GlslLexer.TokenType.IDENTIFIER)) {
                String identifier = reader.peek(-1).value();
                GlslNode expression = null;

                int cursor = reader.getCursor();
                if (reader.tryConsume(GlslLexer.TokenType.EQUAL)) {
                    expression = parseConditionalExpression(reader);
                    if (expression == null) {
                        reader.setCursor(cursor);
                    }
                }

                qualifier = GlslTypeQualifier.identifierLayoutId(identifier, expression);
            }

            if (qualifier != null && reader.tryConsume(GlslLexer.TokenType.SHARED)) {
                qualifier = GlslTypeQualifier.sharedLayoutId();
            }

            if (qualifier == null) {
                break;
            }

            layoutQualifierIds.add(qualifier);
            if (!reader.tryConsume(GlslLexer.TokenType.COMMA)) {
                break;
            }
        }

        // RIGHT_PAREN
        if (!reader.tryConsume(GlslLexer.TokenType.RIGHT_PAREN)) {
            reader.setCursor(layoutCursor);
            return null;
        }

        return GlslTypeQualifier.layout(layoutQualifierIds.toArray(GlslTypeQualifier.LayoutId[]::new));
    }

    private static @Nullable List<GlslTypeQualifier> parseTypeQualifiers(GlslTokenReader reader) {
        // single_type_qualifier
        // type_qualifier single_type_qualifier

        List<GlslTypeQualifier> typeQualifiers = new ArrayList<>();
        while (reader.canRead()) {
            // storage_qualifier
            GlslTypeQualifier storageQualifier = parseStorageQualifier(reader);
            if (storageQualifier != null) {
                typeQualifiers.add(storageQualifier);
                continue;
            }

            // layout_qualifier
            GlslTypeQualifier layoutQualifier = parseLayoutQualifier(reader);
            if (layoutQualifier != null) {
                typeQualifiers.add(layoutQualifier);
                continue;
            }

            // precision_qualifier
            GlslTypeQualifier precisionQualifier = parsePrecisionQualifier(reader);
            if (precisionQualifier != null) {
                typeQualifiers.add(precisionQualifier);
                continue;
            }

            // interpolation_qualifier
            GlslTypeQualifier interpolationQualifier = parseInterpolationQualifier(reader);
            if (interpolationQualifier != null) {
                typeQualifiers.add(interpolationQualifier);
                continue;
            }

            // invariant_qualifier
            if (reader.tryConsume(GlslLexer.TokenType.INVARIANT)) {
                typeQualifiers.add(GlslTypeQualifier.Invariant.INVARIANT);
                continue;
            }

            // precise_qualifier
            if (reader.tryConsume(GlslLexer.TokenType.PRECISE)) {
                typeQualifiers.add(GlslTypeQualifier.Precise.PRECISE);
                continue;
            }

            break;
        }

        return typeQualifiers.isEmpty() ? null : typeQualifiers;
    }

    private static @Nullable GlslTypeQualifier parseStorageQualifier(GlslTokenReader reader) {
        GlslTypeQualifier.StorageType storageQualifier = reader.peek().type().asStorageQualifier();
        if (storageQualifier == null) {
            return null;
        }
        reader.skip();

        // SUBROUTINE LEFT_PAREN type_name_list RIGHT_PAREN
        if (storageQualifier == GlslTypeQualifier.StorageType.SUBROUTINE) {
            if (reader.tryConsume(GlslLexer.TokenType.LEFT_PAREN)) {
                List<String> typeNames = new ArrayList<>();
                while (reader.canRead()) {
                    if (!reader.tryConsume(GlslLexer.TokenType.IDENTIFIER)) {
                        break;
                    }

                    typeNames.add(reader.peek(-1).value());
                    if (!reader.tryConsume(GlslLexer.TokenType.COMMA)) {
                        break;
                    }
                }
                return GlslTypeQualifier.storage(typeNames.toArray(String[]::new));
            }
            return GlslTypeQualifier.storage(new String[0]);
        }

        return GlslTypeQualifier.storage(storageQualifier);
    }

    private static @Nullable GlslNode parseTypeNameList(GlslTokenReader reader) {
        // TYPE_NAME
        // type_name_list COMMA TYPE_NAME
        return null; // TODO
    }

    private static @Nullable GlslTypeSpecifier parseTypeSpecifier(GlslTokenReader reader) {
        // type_specifier_nonarray
        // type_specifier_nonarray array_specifier

        if (!reader.canRead()) {
            return null;
        }

        // type_specifier_nonarray : VOID | FLOAT | DOUBLE | INT | UINT | BOOL | VEC2 | VEC3 | VEC4 | DVEC2 | DVEC3 | DVEC4 | BVEC2 | BVEC3 | BVEC4 | IVEC2 | IVEC3 | IVEC4 | UVEC2 | UVEC3 | UVEC4 | MAT2 | MAT3 | MAT4 | MAT2X2 | MAT2X3 | MAT2X4 | MAT3X2 | MAT3X3 | MAT3X4 | MAT4X2 | MAT4X3 | MAT4X4 | DMAT2 | DMAT3 | DMAT4 | DMAT2X2 | DMAT2X3 | DMAT2X4 | DMAT3X2 | DMAT3X3 | DMAT3X4 | DMAT4X2 | DMAT4X3 | DMAT4X4 | ATOMIC_UINT | SAMPLER2D | SAMPLER3D | SAMPLERCUBE | SAMPLER2DSHADOW | SAMPLERCUBESHADOW | SAMPLER2DARRAY | SAMPLER2DARRAYSHADOW | SAMPLERCUBEARRAY | SAMPLERCUBEARRAYSHADOW | ISAMPLER2D | ISAMPLER3D | ISAMPLERCUBE | ISAMPLER2DARRAY | ISAMPLERCUBEARRAY | USAMPLER2D | USAMPLER3D | USAMPLERCUBE | USAMPLER2DARRAY | USAMPLERCUBEARRAY | SAMPLER1D | SAMPLER1DSHADOW | SAMPLER1DARRAY | SAMPLER1DARRAYSHADOW | ISAMPLER1D | ISAMPLER1DARRAY | USAMPLER1D | USAMPLER1DARRAY | SAMPLER2DRECT | SAMPLER2DRECTSHADOW | ISAMPLER2DRECT | USAMPLER2DRECT | SAMPLERBUFFER | ISAMPLERBUFFER | USAMPLERBUFFER | SAMPLER2DMS | ISAMPLER2DMS | USAMPLER2DMS | SAMPLER2DMSARRAY | ISAMPLER2DMSARRAY | USAMPLER2DMSARRAY | IMAGE2D | IIMAGE2D | UIMAGE2D | IMAGE3D | IIMAGE3D | UIMAGE3D | IMAGECUBE | IIMAGECUBE | UIMAGECUBE | IMAGEBUFFER | IIMAGEBUFFER | UIMAGEBUFFER | IMAGE1D | IIMAGE1D | UIMAGE1D | IMAGE1DARRAY | IIMAGE1DARRAY | UIMAGE1DARRAY | IMAGE2DRECT | IIMAGE2DRECT | UIMAGE2DRECT | IMAGE2DARRAY | IIMAGE2DARRAY | UIMAGE2DARRAY | IMAGECUBEARRAY | IIMAGECUBEARRAY | UIMAGECUBEARRAY | IMAGE2DMS | IIMAGE2DMS | UIMAGE2DMS | IMAGE2DMSARRAY | IIMAGE2DMSARRAY | UIMAGE2DMSARRAY | struct_specifier | TYPE_NAME
        GlslTypeSpecifier typeSpecifier;
        GlslLexer.Token token = reader.peek();
        GlslTypeSpecifier.BuiltinType type = token.type().asBuiltinType();
        if (type != null) {
            typeSpecifier = GlslTypeSpecifier.simple(type);
            reader.skip();
        } else {
            GlslStructSpecifier structSpecifier = parseStructSpecifier(reader);
            if (structSpecifier != null) {
                typeSpecifier = GlslTypeSpecifier.struct(structSpecifier);
            } else {
                if (token.type() == GlslLexer.TokenType.IDENTIFIER) {
                    reader.skip();
                    typeSpecifier = GlslTypeSpecifier.named(token.value());
                } else {
                    return null;
                }
            }
        }

        // type_specifier_nonarray array_specifier
        GlslTypeSpecifier arraySpecifier = parseArraySpecifier(reader, typeSpecifier);
        if (arraySpecifier != null) {
            return arraySpecifier;
        }

        return typeSpecifier;
    }

    private static @Nullable GlslSpecifiedType parseArraySpecifier(GlslTokenReader reader, GlslSpecifiedType type) {
        GlslTypeSpecifier arraySpecifier = parseArraySpecifier(reader, type.getType());
        if (arraySpecifier != null) {
            return new GlslSpecifiedType(arraySpecifier, type.getQualifiers());
        }

        return null;
    }

    private static @Nullable GlslTypeSpecifier parseArraySpecifier(GlslTokenReader reader, GlslTypeSpecifier type) {
        int cursor = reader.getCursor();

        if (reader.tryConsume(GlslLexer.TokenType.LEFT_BRACKET)) {
            // LEFT_BRACKET RIGHT_BRACKET
            if (reader.tryConsume(GlslLexer.TokenType.RIGHT_BRACKET)) {
                return GlslTypeSpecifier.array(type, null);
            }

            // LEFT_BRACKET conditional_expression RIGHT_BRACKET
            GlslNode conditionalExpression = parseConditionalExpression(reader);
            if (conditionalExpression != null && reader.tryConsume(GlslLexer.TokenType.RIGHT_BRACKET)) {
                return GlslTypeSpecifier.array(type, conditionalExpression);
            }

            reader.setCursor(cursor);
        }

        // Impossible ???
        // array_specifier LEFT_BRACKET RIGHT_BRACKET
        // array_specifier LEFT_BRACKET conditional_expression RIGHT_BRACKET

        return null;
    }

    private static @Nullable GlslTypeQualifier.Precision parsePrecisionQualifier(GlslTokenReader reader) {
        // HIGH_PRECISION
        // MEDIUM_PRECISION
        // LOW_PRECISION

        GlslLexer.TokenType type = reader.peekType(0);
        GlslTypeQualifier.Precision precisionQualifier = type != null ? type.asPrecisionQualifier() : null;
        if (precisionQualifier != null) {
            reader.skip();
            return precisionQualifier;
        }
        return null;
    }

    private static @Nullable GlslStructSpecifier parseStructSpecifier(GlslTokenReader reader) {
        // STRUCT IDENTIFIER LEFT_BRACE struct_declaration_list RIGHT_BRACE
        // STRUCT LEFT_BRACE struct_declaration_list RIGHT_BRACE
        return null; // TODO
    }

    private static @Nullable GlslNode parseStructDeclarationList(GlslTokenReader reader) {
        // struct_declaration
        // struct_declaration_list struct_declaration
        return null; // TODO
    }

    private static @Nullable GlslStructSpecifier parseStructDeclaration(GlslTokenReader reader) {
        // type_specifier struct_declarator_list SEMICOLON
        // type_qualifier type_specifier struct_declarator_list SEMICOLON
        return null; // TODO
    }

    private static @Nullable GlslNode parseStructDeclaratorList(GlslTokenReader reader) {
        // struct_declarator
        // struct_declarator_list COMMA struct_declarator
        return null; // TODO
    }

    private static @Nullable GlslNode parseStructDeclarator(GlslTokenReader reader) {
        // IDENTIFIER
        // IDENTIFIER array_specifier
        return null; // TODO
    }

    private static @Nullable GlslNode parseInitializer(GlslTokenReader reader) {
        // assignment_expression
        GlslNode assignmentExpression = parseAssignmentExpression(reader);
        if (assignmentExpression != null) {
            return assignmentExpression;
        }

        // LEFT_BRACE initializer_list RIGHT_BRACE
        // LEFT_BRACE initializer_list COMMA RIGHT_BRACE
        int cursor = reader.getCursor();
        if (reader.tryConsume(GlslLexer.TokenType.LEFT_BRACE)) {
            List<GlslNode> initializerList = parseInitializerList(reader);
            reader.tryConsume(GlslLexer.TokenType.COMMA);
            if (reader.tryConsume(GlslLexer.TokenType.RIGHT_BRACE)) {
                return GlslNode.compound(initializerList);
            }
        }

        reader.setCursor(cursor);
        return null;
    }

    private static List<GlslNode> parseInitializerList(GlslTokenReader reader) {
        // initializer
        // initializer_list COMMA initializer

        int cursor = reader.getCursor();
        List<GlslNode> initializers = new ArrayList<>();
        while (reader.canRead()) {
            GlslNode statement = parseStatement(reader);
            if (statement == null) {
                reader.setCursor(cursor);
                break;
            }

            initializers.add(statement);
            cursor = reader.getCursor();

            if (!reader.tryConsume(GlslLexer.TokenType.COMMA)) {
                break;
            }
        }

        return initializers;
    }

    private static @Nullable GlslNode parseStatement(GlslTokenReader reader) {
        // compound_statement
        // simple_statement

        int cursor = reader.getCursor();

        // compound_statement
        GlslNode compoundStatement = parseCompoundStatement(reader);
        if (compoundStatement != null) {
            return compoundStatement;
        }
        reader.setCursor(cursor);

        // simple_statement
        GlslNode simpleStatement = parseSimpleStatement(reader);
        if (simpleStatement != null) {
            return simpleStatement;
        }
        reader.setCursor(cursor);

        return null;
    }

    private static @Nullable GlslNode parseSimpleStatement(GlslTokenReader reader) {
        // declaration_statement
        // expression_statement
        // selection_statement
        // switch_statement
        // case_label
        // iteration_statement
        // jump_statement

        int cursor = reader.getCursor();
        GlslNode statement;

        // declaration_statement -> declaration
        statement = parseDeclaration(reader);
        if (statement != null) {
            return statement;
        }
        reader.setCursor(cursor);

        // expression_statement
        statement = parseExpressionStatement(reader);
        if (statement != null) {
            return statement;
        }
        reader.setCursor(cursor);

        // selection_statement
        statement = parseSelectionStatement(reader);
        if (statement != null) {
            return statement;
        }
        reader.setCursor(cursor);

        statement = parseSwitchStatement(reader);
        if (statement != null) {
            return statement;
        }
        reader.setCursor(cursor);

        statement = parseCaseLabel(reader);
        if (statement != null) {
            return statement;
        }
        reader.setCursor(cursor);

        statement = parseIterationStatement(reader);
        if (statement != null) {
            return statement;
        }
        reader.setCursor(cursor);

        statement = parseJumpStatement(reader);
        if (statement != null) {
            return statement;
        }
        reader.setCursor(cursor);

        return null;
    }

    private static @Nullable GlslNode parseCompoundStatement(GlslTokenReader reader) {
        // LEFT_BRACE RIGHT_BRACE
        // LEFT_BRACE statement_list RIGHT_BRACE

        int cursor = reader.getCursor();
        if (!reader.tryConsume(GlslLexer.TokenType.LEFT_BRACE)) {
            reader.setCursor(cursor);
            return null;
        }

        if (reader.tryConsume(GlslLexer.TokenType.RIGHT_BRACE)) {
            return GlslEmptyNode.INSTANCE;
        }

        // statement_list
        List<GlslNode> statements = parseStatementList(reader);

        if (!reader.tryConsume(GlslLexer.TokenType.RIGHT_BRACE)) {
            reader.setCursor(cursor);
            return null;
        }

        return GlslNode.compound(statements);
    }

    private static @Nullable GlslNode parseStatementNoNewScope(GlslTokenReader reader) {
        // compound_statement_no_new_scope
        GlslNode statementNoNewScope = parseCompoundStatementNoNewScope(reader);
        if (statementNoNewScope != null) {
            return statementNoNewScope;
        }

        // simple_statement
        return parseSimpleStatement(reader);
    }

    private static @Nullable GlslNode parseCompoundStatementNoNewScope(GlslTokenReader reader) {
        // LEFT_BRACE RIGHT_BRACE
        // LEFT_BRACE statement_list RIGHT_BRACE

        int cursor = reader.getCursor();
        if (!reader.tryConsume(GlslLexer.TokenType.LEFT_BRACE)) {
            return null;
        }

        List<GlslNode> statements = parseStatementList(reader);
        if (!reader.tryConsume(GlslLexer.TokenType.RIGHT_BRACE)) {
            reader.setCursor(cursor);
            return null;
        }

        return GlslNode.compound(statements);
    }

    private static List<GlslNode> parseStatementList(GlslTokenReader reader) {
        List<GlslNode> statements = new ArrayList<>();
        while (reader.canRead()) {
            GlslNode statement = parseStatement(reader);
            if (statement == null) {
                break;
            }

            statements.add(statement);
        }

        return statements;
    }

    private static @Nullable GlslNode parseExpressionStatement(GlslTokenReader reader) {
        if (reader.tryConsume(GlslLexer.TokenType.SEMICOLON)) {
            return GlslEmptyNode.INSTANCE;
        }

        int cursor = reader.getCursor();
        GlslNode condition = parseCondition(reader);
        if (condition != null && reader.tryConsume(GlslLexer.TokenType.SEMICOLON)) {
            return condition;
        }
        reader.setCursor(cursor);
        return null;
    }

    private static @Nullable GlslNode parseSelectionStatement(GlslTokenReader reader) {
        // IF LEFT_PAREN condition RIGHT_PAREN statement ELSE statement
        // IF LEFT_PAREN condition RIGHT_PAREN statement

        int cursor = reader.getCursor();
        if (!reader.tryConsume(GlslLexer.TokenType.IF, GlslLexer.TokenType.LEFT_PAREN)) {
            reader.setCursor(cursor);
            return null;
        }

        GlslNode expression = parseExpressionStatement(reader);
        if (expression == null) {
            reader.setCursor(cursor);
            return null;
        }

        if (!reader.tryConsume(GlslLexer.TokenType.RIGHT_PAREN)) {
            reader.setCursor(cursor);
            return null;
        }

        // selection_rest_statement
        GlslNode statement = parseStatement(reader);
        if (statement == null) {
            reader.setCursor(cursor);
            return null;
        }

        cursor = reader.getCursor();
        // statement ELSE statement

        if (reader.tryConsume(GlslLexer.TokenType.ELSE)) {
            GlslNode otherStatement = parseStatement(reader);
            if (otherStatement != null) {
                return new GlslSelectionNode(expression, statement, otherStatement);
            }

            reader.setCursor(cursor);
            return null;
        }

        reader.setCursor(cursor);
        return new GlslSelectionNode(expression, statement, null);
    }

    private static @Nullable GlslNode parseCondition(GlslTokenReader reader) {
        // expression
        GlslNode expression = parseExpression(reader);
        if (expression != null) {
            return expression;
        }

        // fully_specified_type IDENTIFIER EQUAL initializer
        int cursor = reader.getCursor();
        GlslSpecifiedType type = parseFullySpecifiedType(reader);
        if (type != null && reader.tryConsume(GlslLexer.TokenType.IDENTIFIER, GlslLexer.TokenType.EQUAL)) {
            String name = reader.peek(-2).value();
            GlslNode initializer = parseInitializer(reader);
            if (initializer != null) {
                return new GlslNewNode(type, name, initializer);
            }
        }

        reader.setCursor(cursor);
        return null;
    }

    private static @Nullable GlslNode parseSwitchStatement(GlslTokenReader reader) {
        // SWITCH LEFT_PAREN condition RIGHT_PAREN LEFT_BRACE switch_statement_list RIGHT_BRACE
        int cursor = reader.getCursor();
        if (reader.tryConsume(GlslLexer.TokenType.SWITCH, GlslLexer.TokenType.LEFT_PAREN)) {
            GlslNode condition = parseCondition(reader);
            if (condition != null && reader.tryConsume(GlslLexer.TokenType.RIGHT_PAREN, GlslLexer.TokenType.LEFT_BRACE)) {
                List<GlslNode> statements = parseStatementList(reader);
                if (reader.tryConsume(GlslLexer.TokenType.RIGHT_BRACE)) {
                    return new GlslSwitchNode(condition, statements);
                }
            }
        }
        reader.setCursor(cursor);
        return null;
    }

    private static @Nullable GlslCaseLabelNode parseCaseLabel(GlslTokenReader reader) {
        int cursor = reader.getCursor();

        // CASE condition COLON
        if (reader.tryConsume(GlslLexer.TokenType.CASE)) {
            GlslNode condition = parseCondition(reader);
            if (condition != null && reader.tryConsume(GlslLexer.TokenType.COLON)) {
                return new GlslCaseLabelNode(condition);
            }
        }
        reader.setCursor(cursor);

        if (reader.tryConsume(GlslLexer.TokenType.DEFAULT, GlslLexer.TokenType.COLON)) {
            return new GlslCaseLabelNode(null);
        }
        return null;
    }

    private static @Nullable GlslNode parseIterationStatement(GlslTokenReader reader) {
        // WHILE LEFT_PAREN condition RIGHT_PAREN statement_no_new_scope
        // DO statement WHILE LEFT_PAREN condition RIGHT_PAREN SEMICOLON
        // FOR LEFT_PAREN for_init_statement for_rest_statement RIGHT_PAREN statement_no_new_scope
        return null; // TODO
    }

    private static @Nullable GlslNode parseForInitStatement(GlslTokenReader reader) {
        // expression_statement
        // declaration_statement
        return null; // TODO
    }

    private static @Nullable GlslNode parseConditionopt(GlslTokenReader reader) {
        // condition
        return null; // TODO
    }

    private static @Nullable GlslNode parseForRestStatement(GlslTokenReader reader) {
        // conditionopt SEMICOLON
        // conditionopt SEMICOLON condition
        return null; // TODO
    }

    private static @Nullable GlslNode parseJumpStatement(GlslTokenReader reader) {
        // CONTINUE SEMICOLON
        // BREAK SEMICOLON
        // RETURN SEMICOLON
        // RETURN condition SEMICOLON
        // DISCARD SEMICOLON
        return null; // TODO
    }

    private static @Nullable GlslFunction parseFunctionDefinition(GlslTokenReader reader) {
        int cursor = reader.getCursor();

        GlslFunctionHeader functionPrototype = parseFunctionPrototype(reader);
        if (functionPrototype == null) {
            return null;
        }

        GlslNode statement = parseCompoundStatementNoNewScope(reader);
        if (statement == null) {
            reader.setCursor(cursor);
            return null;
        }

        // function_prototype compound_statement_no_new_scope
        return new GlslFunction(functionPrototype, statement);
    }
}
