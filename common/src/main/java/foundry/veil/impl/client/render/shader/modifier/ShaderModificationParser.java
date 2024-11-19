package foundry.veil.impl.client.render.shader.modifier;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public final class ShaderModificationParser {

    public static ShaderModification parse(ShaderModifierLexer.Token[] tokens, boolean vertex) throws ShaderModificationSyntaxException {
        TokenReader reader = new TokenReader(tokens);
        reader.skipWhitespace(); // Skip comments, garbage, etc

        int version = -1;
        int priority = 1000;
        List<ResourceLocation> includes = new ArrayList<>();
        while (reader.canRead()) {
            switch (reader.peek().type()) {
                case VERSION -> {
                    if (version != -1) {
                        throw error("Version can only be set once", reader);
                    }
                    reader.skip();
                    version = consumeInt(reader);
                    reader.skipWhitespace();
                    continue;
                }
                case PRIORITY -> {
                    reader.skip();
                    priority = consumeInt(reader);
                    reader.skipWhitespace();
                    continue;
                }
                case REPLACE -> {
                    reader.skip();
                    ResourceLocation file = consumeLocation(reader);
                    reader.skipWhitespace();
                    if (reader.canRead()) {
                        throw error("Trailing statement", reader);
                    }
                    return new ReplaceShaderModification(priority, file);
                }
                case INCLUDE -> {
                    while (reader.canRead() && reader.peek().type() == ShaderModifierLexer.TokenType.INCLUDE) {
                        reader.skip();
                        includes.add(consumeLocation(reader));
                        reader.skipWhitespace();
                    }
                    continue;
                }
            }
            break;
        }

        Context context = new Context(new ArrayList<>(), new StringBuilder(), new StringBuilder(), new HashMap<>());
        while (reader.canRead()) {
            switch (reader.peek().type()) {
                case COMMENT, NEWLINE -> reader.skipWhitespace();
                case LEFT_BRACKET -> {
                    reader.skip();
                    parseCommand(reader, context, vertex);
                }
                default -> throw error("Unexpected Token", reader);
            }
        }

        ShaderModification.Function[] functions = new ShaderModification.Function[context.functions.size()];
        int i = 0;
        for (Map.Entry<FunctionInject, StringBuilder> entry : context.functions.entrySet()) {
            FunctionInject inject = entry.getKey();
            functions[i] = ShaderModification.Function.create(inject.name, inject.parameters, inject.head, entry.getValue().toString());
            i++;
        }

        return vertex ?
                new VertexShaderModification(version,
                        priority,
                        includes.toArray(ResourceLocation[]::new),
                        context.output.toString().trim(),
                        context.uniform.toString().trim(),
                        functions,
                        context.attributes.toArray(VertexShaderModification.Attribute[]::new)
                ) :
                new SimpleShaderModification(version,
                        priority,
                        includes.toArray(ResourceLocation[]::new),
                        context.output.toString().trim(),
                        context.uniform.toString().trim(),
                        functions);
    }

    private static void parseCommand(TokenReader reader, Context context, boolean vertex) throws ShaderModificationSyntaxException {
        switch (reader.peek().type()) {
            case GET_ATTRIBUTE -> {
                if (!vertex) {
                    throw error("Only vertex shader modifications can get attributes", reader);
                }

                reader.skip();
                int index = consumeInt(reader);
                consume(reader, ShaderModifierLexer.TokenType.RIGHT_BRACKET);
                String definition = consume(reader, ShaderModifierLexer.TokenType.DEFINITION);

                Matcher matcher = ShaderModifierLexer.TokenType.DEFINITION.getPattern().matcher(definition);
                if (!matcher.matches()) {
                    throw new IllegalStateException();
                }

                context.attributes.add(new VertexShaderModification.Attribute(index, matcher.group(1), matcher.group(2)));
            }
            case OUTPUT -> {
                reader.skip();
                consume(reader, ShaderModifierLexer.TokenType.RIGHT_BRACKET);

                reader.skipWhitespace();
                context.output.append(consumeGLSL(reader));
            }
            case UNIFORM -> {
                reader.skip();
                consume(reader, ShaderModifierLexer.TokenType.RIGHT_BRACKET);

                reader.skipWhitespace();
                context.uniform.append(consumeGLSL(reader));
            }
            case FUNCTION -> {
                reader.skip();

                String name = consume(reader, ShaderModifierLexer.TokenType.ALPHANUMERIC);
                int parameters = -1;
                if (reader.peek().type() == ShaderModifierLexer.TokenType.LEFT_PARENTHESIS) {
                    reader.skip();
                    parameters = consumeInt(reader);
                    consume(reader, ShaderModifierLexer.TokenType.RIGHT_PARENTHESIS);
                }

                boolean head;
                if (reader.peek().type() == ShaderModifierLexer.TokenType.HEAD) {
                    reader.skip();
                    head = true;
                } else {
                    consume(reader, ShaderModifierLexer.TokenType.TAIL);
                    head = false;
                }

                consume(reader, ShaderModifierLexer.TokenType.RIGHT_BRACKET);
                reader.skipWhitespace();
                context.functions.computeIfAbsent(new FunctionInject(name, parameters, head), unused -> new StringBuilder()).append(consumeGLSL(reader));
            }
            default -> throw error("Unexpected Token: " + reader.peek(), reader);
        }
    }

    private static String consumeGLSL(TokenReader reader) {
        StringBuilder code = new StringBuilder();
        while (reader.canRead()) {
            ShaderModifierLexer.Token token = reader.peek();
            if (token.type() == ShaderModifierLexer.TokenType.LEFT_BRACKET) {
                ShaderModifierLexer.Token next = reader.peek(1);
                if (next != null && next.type().isCommand()) {
                    // Only stop if a new command is detected
                    break;
                }
            }

            code.append(token.value());
            if (token.type() != ShaderModifierLexer.TokenType.NEWLINE) {
                code.append(' ');
            }
            reader.skip();
        }
        return code.toString().trim() + '\n';
    }

    private static ResourceLocation consumeLocation(TokenReader reader) throws ShaderModificationSyntaxException {
        String namespace = consume(reader, ShaderModifierLexer.TokenType.ALPHANUMERIC);
        if (reader.peek().type() == ShaderModifierLexer.TokenType.COLON) {
            reader.skip();

            StringBuilder path = new StringBuilder();
            while (reader.canRead() && reader.peek().type().isValidLocation()) {
                path.append(reader.peek().value());
                reader.skip();
            }

            if (path.isEmpty()) {
                throw error("Unexpected Token", reader);
            }

            return ResourceLocation.fromNamespaceAndPath(namespace, path.toString());
        }
        return ResourceLocation.parse(namespace);
    }

    private static int consumeInt(TokenReader reader) throws ShaderModificationSyntaxException {
        return Integer.parseInt(consume(reader, ShaderModifierLexer.TokenType.NUMERAL));
    }

    private static String consume(TokenReader reader, ShaderModifierLexer.TokenType token) throws ShaderModificationSyntaxException {
        expect(reader, token);
        String value = reader.peek().value();
        reader.skip();
        return value;
    }

    private static void expect(TokenReader reader, ShaderModifierLexer.TokenType token) throws ShaderModificationSyntaxException {
        if (!reader.canRead() || reader.peek().type() != token) {
            throw error("Expected " + token, reader);
        }
    }

    private static ShaderModificationSyntaxException error(String error, TokenReader reader) {
        return new ShaderModificationSyntaxException(error, reader.getString(), reader.getCursorOffset());
    }

    private record Context(List<VertexShaderModification.Attribute> attributes,
                           StringBuilder output,
                           StringBuilder uniform,
                           Map<FunctionInject, StringBuilder> functions) {
    }

    private record FunctionInject(String name, int parameters, boolean head) {
    }

    private static class TokenReader {

        private final ShaderModifierLexer.Token[] tokens;
        private int cursor;

        public TokenReader(ShaderModifierLexer.Token[] tokens) {
            this.tokens = tokens;
        }

        public String getString() {
            StringBuilder builder = new StringBuilder();
            for (ShaderModifierLexer.Token token : this.tokens) {
                builder.append(token.value());
            }
            return builder.toString();
        }

        public boolean canRead(int length) {
            return this.cursor + length <= this.tokens.length;
        }

        public boolean canRead() {
            return this.canRead(1);
        }

        public int getCursorOffset() {
            int offset = 0;
            for (int i = 0; i <= Math.min(this.cursor, this.tokens.length - 1); i++) {
                offset += this.tokens[i].value().length();
            }
            return offset;
        }

        public ShaderModifierLexer.Token peek() {
            return this.peek(0);
        }

        public @Nullable ShaderModifierLexer.Token peek(int amount) {
            return this.cursor + amount < this.tokens.length ? this.tokens[this.cursor + amount] : null;
        }

        public void skip() {
            this.cursor++;
        }

        public void skipWhitespace() {
            while (this.canRead() && this.peek().type().isWhitespace()) {
                this.skip();
            }
        }
    }
}
