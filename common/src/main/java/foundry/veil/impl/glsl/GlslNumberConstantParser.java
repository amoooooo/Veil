package foundry.veil.impl.glsl;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@ApiStatus.Internal
public final class GlslNumberConstantParser {

    private GlslNumberConstantParser() {
    }

    static @Nullable GlslLexer.Token parseNumberConstant(GlslLexer.StringReader reader) {
        if (!reader.canRead()) {
            return null;
        }
        GlslLexer.Token floatingConstant = parseFloatingConstant(reader);
        if (floatingConstant != null) {
            return floatingConstant;
        }
        return parseIntegerConstant(reader);
    }

    private static @Nullable GlslLexer.Token parseFloatingConstant(GlslLexer.StringReader reader) {
        // digit-sequence . digit-sequence exponent-partopt floating-suffixopt
        // digit-sequence . exponent-partopt floating-suffixopt
        // . digit-sequence exponent-partopt floating-suffixopt
        // digit-sequence exponent-part floating-suffixopt

        StringBuilder fractionalConstant = parseFractionalConstant(reader);
        if (fractionalConstant != null) {
            String exponent = parseExponentPart(reader);
            if (exponent != null) {
                fractionalConstant.append(exponent);
            }
            GlslLexer.TokenType type = getFloatingType(reader);
            return new GlslLexer.Token(type, fractionalConstant.toString());
        }

        int cursor = reader.cursor;
        StringBuilder sequence = parseDigitSequence(reader);
        if (sequence != null) {
            String exponent = parseExponentPart(reader);
            if (exponent != null) {
                sequence.append(exponent);
                GlslLexer.TokenType type = getFloatingType(reader);
                return new GlslLexer.Token(type, sequence.toString());
            }
        }

        reader.cursor = cursor;
        return null;
    }

    private static @Nullable StringBuilder parseFractionalConstant(GlslLexer.StringReader reader) {
        int cursor = reader.cursor;
        StringBuilder first = parseDigitSequence(reader);
        if (reader.chars[reader.cursor] != '.') {
            reader.cursor = cursor;
            return null;
        }
        reader.skip();

        StringBuilder second = parseDigitSequence(reader);
        if (first != null) {
            first.append('.');
            if (second != null) {
                first.append(second);
            }
            return first;
        } else {
            if (second == null) {
                return null;
            }
            return new StringBuilder(".").append(second);
        }
    }

    private static @Nullable StringBuilder parseDigitSequence(GlslLexer.StringReader reader) {
        int i = reader.cursor;
        char[] chars = reader.chars;

        StringBuilder sequence = new StringBuilder();
        while (i < chars.length && (Character.isDigit(chars[i]) || Character.isWhitespace(chars[i]))) {
            if (!Character.isWhitespace(chars[i])) {
                sequence.append(chars[i]);
            }
            i++;
        }

        if (sequence.isEmpty()) {
            return null;
        }

        reader.skip(i - reader.cursor);
        return sequence;
    }

    private static @Nullable String parseExponentPart(GlslLexer.StringReader reader) {
        int cursor = reader.cursor;
        char[] chars = reader.chars;
        if (chars[cursor] != 'e' && chars[cursor] != 'E') {
            return null;
        }

        reader.skip();
        if (!reader.canRead()) {
            return null;
        }

        char sign = chars[cursor + 1];
        if (sign == '+' || sign == '-') {
            reader.skip();
            StringBuilder digitSequence = parseDigitSequence(reader);
            if (digitSequence == null) {
                reader.cursor = cursor;
                return null;
            }

            return "e" + sign + digitSequence;
        }

        StringBuilder digitSequence = parseDigitSequence(reader);
        if (digitSequence == null) {
            reader.cursor = cursor;
            return null;
        }

        return "e" + digitSequence;
    }

    private static GlslLexer.TokenType getFloatingType(GlslLexer.StringReader reader) {
        if (!reader.canRead()) {
            return GlslLexer.TokenType.FLOATING_CONSTANT;
        }

        char[] chars = reader.chars;
        char first = chars[reader.cursor];
        if (first == 'f' || first == 'F') {
            reader.skip();
            return GlslLexer.TokenType.FLOATING_CONSTANT;
        }

        if (reader.cursor + 1 >= chars.length) {
            return GlslLexer.TokenType.FLOATING_CONSTANT;
        }

        char second = chars[reader.cursor + 1];
        if ((first == 'l' && second == 'f') || (first == 'L' && second == 'F')) {
            reader.skip(2);
            return GlslLexer.TokenType.DOUBLE_CONSTANT;
        }

        return GlslLexer.TokenType.FLOATING_CONSTANT;
    }

    private static @Nullable GlslLexer.Token parseIntegerConstant(GlslLexer.StringReader reader) {
        // decimal-constant integer-suffixopt
        // octal-constant integer-suffixopt
        // hexadecimal-constant integer-suffixopt

        char[] chars = reader.chars;
        int cursor = reader.cursor;
        StringBuilder numberBuilder = new StringBuilder();
        GlslLexer.TokenType type = null;

        // Decimal
        if (chars[cursor] >= '1' && chars[cursor] <= '9') {
            type = GlslLexer.TokenType.INTEGER_DECIMAL_CONSTANT;
            numberBuilder.append(chars[cursor]);
            reader.skip();
            consumeDigits(reader, numberBuilder, '9');
        } else if (chars[cursor] == '0') {
            reader.skip();

            // Hexadecimal
            if (cursor + 1 < chars.length && (chars[cursor + 1] == 'x' || chars[cursor + 1] == 'X')) {
                type = GlslLexer.TokenType.INTEGER_HEXADECIMAL_CONSTANT;
                reader.skip();

                cursor = reader.cursor;
                while (cursor < chars.length && (chars[cursor] >= '0' && chars[cursor] <= '9') || (chars[cursor] >= 'a' && chars[cursor] <= 'f') || (chars[cursor] >= 'A' && chars[cursor] <= 'F')) {
                    if (!Character.isWhitespace(chars[cursor])) {
                        numberBuilder.append(chars[cursor]);
                    }
                    cursor++;
                }
                reader.cursor = cursor;
            } else {
                // Octal
                type = GlslLexer.TokenType.INTEGER_OCTAL_CONSTANT;
                numberBuilder.append('0');
                consumeDigits(reader, numberBuilder, '7');
            }
        }

        if (numberBuilder.isEmpty()) {
            return null;
        }

        if (reader.canRead() && (chars[reader.cursor] == 'u' || chars[reader.cursor] == 'U')) {
            switch (Objects.requireNonNull(type)) {
                case INTEGER_HEXADECIMAL_CONSTANT -> type = GlslLexer.TokenType.UINTEGER_HEXADECIMAL_CONSTANT;
                case INTEGER_OCTAL_CONSTANT -> type = GlslLexer.TokenType.UINTEGER_OCTAL_CONSTANT;
                case INTEGER_DECIMAL_CONSTANT -> type = GlslLexer.TokenType.UINTEGER_DECIMAL_CONSTANT;
            }
            reader.skip();
        }

        return new GlslLexer.Token(type, numberBuilder.toString());
    }

    private static void consumeDigits(GlslLexer.StringReader reader, StringBuilder builder, char end) {
        char[] chars = reader.chars;
        int cursor = reader.cursor;
        while (cursor < chars.length && (chars[cursor] >= '0' && chars[cursor] <= end)) {
            builder.append(chars[cursor]);
            cursor++;
        }
        reader.cursor = cursor;
    }
}
