/*
 * Anarres C Preprocessor
 * Copyright (c) 2007-2015, Shevek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package foundry.veil.lib.anarres.cpp;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;

import static foundry.veil.lib.anarres.cpp.Token.*;

/*
 * NOTE: This File was edited by the Veil Team based on this commit: https://github.com/shevek/jcpp/commit/5e50e75ec33f5b4567cabfd60b6baca39524a8b7
 *
 * - Updated formatting to more closely follow project standards
 * - Removed all file/IO
 * - Fixed minor errors
 */

/**
 * Does not handle digraphs.
 */
public class LexerSource extends Source {

    private JoinReader reader;
    private final boolean ppvalid;
    private boolean bol;
    private boolean include;

    private boolean digraphs;

    /* Unread. */
    private int u0, u1;
    private int ucount;

    private int line;
    private int column;
    private int lastcolumn;
    private boolean cr;

    /* ppvalid is:
     * false in StringLexerSource,
     * true in FileLexerSource */
    public LexerSource(Reader r, boolean ppvalid) {
        this.reader = new JoinReader(r);
        this.ppvalid = ppvalid;
        this.bol = true;
        this.include = false;

        this.digraphs = true;

        this.ucount = 0;

        this.line = 1;
        this.column = 0;
        this.lastcolumn = -1;
        this.cr = false;
    }

    @Override
        /* pp */ void init(Preprocessor pp) {
        super.init(pp);
        this.digraphs = pp.getFeature(Feature.DIGRAPHS);
        this.reader.init(pp, this);
    }

    /**
     * Returns the line number of the last read character in this source.
     * <p>
     * Lines are numbered from 1.
     *
     * @return the line number of the last read character in this source.
     */
    @Override
    public int getLine() {
        return line;
    }

    /**
     * Returns the column number of the last read character in this source.
     * <p>
     * Columns are numbered from 0.
     *
     * @return the column number of the last read character in this source.
     */
    @Override
    public int getColumn() {
        return column;
    }

    @Override
        /* pp */ boolean isNumbered() {
        return true;
    }

    /* Error handling. */
    private void _error(String msg, boolean error)
            throws LexerException {
        int _l = line;
        int _c = column;
        if (_c == 0) {
            _c = lastcolumn;
            _l--;
        } else {
            _c--;
        }
        if (error) {
            super.error(_l, _c, msg);
        } else {
            super.warning(_l, _c, msg);
        }
    }

    /* Allow JoinReader to call this. */
    /* pp */
    final void error(String msg)
            throws LexerException {
        this._error(msg, true);
    }

    /* Allow JoinReader to call this. */
    /* pp */
    final void warning(String msg)
            throws LexerException {
        this._error(msg, false);
    }

    /* A flag for string handling. */

    /* pp */ void setInclude(boolean b) {
        this.include = b;
    }

    /*
     * private boolean _isLineSeparator(int c) {
     * return Character.getType(c) == Character.LINE_SEPARATOR
     * || c == -1;
     * }
     */

    /* XXX Move to JoinReader and canonicalise newlines. */
    private static boolean isLineSeparator(int c) {
        switch ((char) c) {
            case '\r':
            case '\n':
            case '\u2028':
            case '\u2029':
            case '\u000B':
            case '\u000C':
            case '\u0085':
                return true;
            default:
                return (c == -1);
        }
    }

    private int read()
            throws IOException,
            LexerException {
        int c;
        assert ucount <= 2 : "Illegal ucount: " + ucount;
        switch (ucount) {
            case 2:
                ucount = 1;
                c = u1;
                break;
            case 1:
                ucount = 0;
                c = u0;
                break;
            default:
                if (reader == null) {
                    c = -1;
                } else {
                    c = reader.read();
                }
                break;
        }

        switch (c) {
            case '\r':
                cr = true;
                line++;
                lastcolumn = column;
                column = 0;
                break;
            case '\n':
                if (cr) {
                    cr = false;
                    break;
                }
                /* fallthrough */
            case '\u2028':
            case '\u2029':
            case '\u000B':
            case '\u000C':
            case '\u0085':
                cr = false;
                line++;
                lastcolumn = column;
                column = 0;
                break;
            case -1:
                cr = false;
                break;
            default:
                cr = false;
                column++;
                break;
        }

        /*
         * if (isLineSeparator(c)) {
         * line++;
         * lastcolumn = column;
         * column = 0;
         * }
         * else {
         * column++;
         * }
         */
        return c;
    }

    /* You can unget AT MOST one newline. */
    private void unread(int c)
            throws IOException {
        /* XXX Must unread newlines. */
        if (c != -1) {
            if (isLineSeparator(c)) {
                line--;
                column = lastcolumn;
                cr = false;
            } else {
                column--;
            }
            switch (ucount) {
                case 0:
                    u0 = c;
                    ucount = 1;
                    break;
                case 1:
                    u1 = c;
                    ucount = 2;
                    break;
                default:
                    throw new IllegalStateException(
                            "Cannot unget another character!"
                    );
            }
            // reader.unread(c);
        }
    }

    /* Consumes the rest of the current line into an invalid. */
    @NotNull
    private Token invalid(StringBuilder text, String reason)
            throws IOException,
            LexerException {
        int d = this.read();
        while (!isLineSeparator(d)) {
            text.append((char) d);
            d = this.read();
        }
        this.unread(d);
        return new Token(INVALID, text.toString(), reason);
    }

    @NotNull
    private Token ccomment()
            throws IOException,
            LexerException {
        StringBuilder text = new StringBuilder("/*");
        int d;
        do {
            do {
                d = this.read();
                if (d == -1) {
                    return new Token(INVALID, text.toString(),
                            "Unterminated comment");
                }
                text.append((char) d);
            } while (d != '*');
            do {
                d = this.read();
                if (d == -1) {
                    return new Token(INVALID, text.toString(),
                            "Unterminated comment");
                }
                text.append((char) d);
            } while (d == '*');
        } while (d != '/');
        return new Token(CCOMMENT, text.toString());
    }

    @NotNull
    private Token cppcomment()
            throws IOException,
            LexerException {
        StringBuilder text = new StringBuilder("//");
        int d = this.read();
        while (!isLineSeparator(d)) {
            text.append((char) d);
            d = this.read();
        }
        this.unread(d);
        return new Token(CPPCOMMENT, text.toString());
    }

    /**
     * Lexes an escaped character, appends the lexed escape sequence to 'text' and returns the parsed character value.
     *
     * @param text The buffer to which the literal escape sequence is appended.
     * @return The new parsed character value.
     * @throws IOException    if it goes badly wrong.
     * @throws LexerException if it goes wrong.
     */
    private int escape(StringBuilder text)
            throws IOException,
            LexerException {
        int d = this.read();
        switch (d) {
            case 'a':
                text.append('a');
                return 0x07;
            case 'b':
                text.append('b');
                return '\b';
            case 'f':
                text.append('f');
                return '\f';
            case 'n':
                text.append('n');
                return '\n';
            case 'r':
                text.append('r');
                return '\r';
            case 't':
                text.append('t');
                return '\t';
            case 'v':
                text.append('v');
                return 0x0b;
            case '\\':
                text.append('\\');
                return '\\';

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                int len = 0;
                int val = 0;
                do {
                    val = (val << 3) + Character.digit(d, 8);
                    text.append((char) d);
                    d = this.read();
                } while (++len < 3 && Character.digit(d, 8) != -1);
                this.unread(d);
                return val;

            case 'x':
                text.append((char) d);
                len = 0;
                val = 0;
                while (len++ < 2) {
                    d = this.read();
                    if (Character.digit(d, 16) == -1) {
                        this.unread(d);
                        break;
                    }
                    val = (val << 4) + Character.digit(d, 16);
                    text.append((char) d);
                }
                return val;

            /* Exclude two cases from the warning. */
            case '"':
                text.append('"');
                return '"';
            case '\'':
                text.append('\'');
                return '\'';

            default:
                this.warning("Unnecessary escape character " + (char) d);
                text.append((char) d);
                return d;
        }
    }

    @NotNull
    private Token character()
            throws IOException,
            LexerException {
        StringBuilder text = new StringBuilder("'");
        int d = this.read();
        if (d == '\\') {
            text.append('\\');
            d = this.escape(text);
        } else if (isLineSeparator(d)) {
            this.unread(d);
            return new Token(INVALID, text.toString(),
                    "Unterminated character literal");
        } else if (d == '\'') {
            text.append('\'');
            return new Token(INVALID, text.toString(),
                    "Empty character literal");
        } else if (!Character.isDefined(d)) {
            text.append('?');
            return this.invalid(text, "Illegal unicode character literal");
        } else {
            text.append((char) d);
        }

        int e = this.read();
        if (e != '\'') {
            // error("Illegal character constant");
            /* We consume up to the next ' or the rest of the line. */
            for (; ; ) {
                if (isLineSeparator(e)) {
                    this.unread(e);
                    break;
                }
                text.append((char) e);
                if (e == '\'') {
                    break;
                }
                e = this.read();
            }
            return new Token(INVALID, text.toString(),
                    "Illegal character constant " + text);
        }
        text.append('\'');
        /* XXX It this a bad cast? */
        return new Token(CHARACTER,
                text.toString(), Character.valueOf((char) d));
    }

    @NotNull
    private Token string(char open, char close)
            throws IOException,
            LexerException {
        StringBuilder text = new StringBuilder();
        text.append(open);

        StringBuilder buf = new StringBuilder();

        for (; ; ) {
            int c = this.read();
            if (c == close) {
                break;
            } else if (c == '\\') {
                text.append('\\');
                if (!include) {
                    char d = (char) this.escape(text);
                    buf.append(d);
                }
            } else if (c == -1) {
                this.unread(c);
                // error("End of file in string literal after " + buf);
                return new Token(INVALID, text.toString(),
                        "End of file in string literal after " + buf);
            } else if (isLineSeparator(c)) {
                this.unread(c);
                // error("Unterminated string literal after " + buf);
                return new Token(INVALID, text.toString(),
                        "Unterminated string literal after " + buf);
            } else {
                text.append((char) c);
                buf.append((char) c);
            }
        }
        text.append(close);
        switch (close) {
            case '"':
                return new Token(STRING,
                        text.toString(), buf.toString());
            case '>':
                return new Token(HEADER,
                        text.toString(), buf.toString());
            case '\'':
                if (buf.length() == 1) {
                    return new Token(CHARACTER,
                            text.toString(), buf.toString());
                }
                return new Token(SQSTRING,
                        text.toString(), buf.toString());
            default:
                throw new IllegalStateException(
                        "Unknown closing character " + close);
        }
    }

    @NotNull
    private Token _number_suffix(StringBuilder text, NumericValue value, int d)
            throws IOException,
            LexerException {
        int flags = 0;    // U, I, L, LL, F, D, MSB
        for (; ; ) {
            if (d == 'U' || d == 'u') {
                if ((flags & NumericValue.F_UNSIGNED) != 0) {
                    this.warning("Duplicate unsigned suffix " + d);
                }
                flags |= NumericValue.F_UNSIGNED;
                text.append((char) d);
                d = this.read();
            } else if (d == 'L' || d == 'l') {
                if ((flags & NumericValue.FF_SIZE) != 0) {
                    this.warning("Multiple length suffixes after " + text);
                }
                text.append((char) d);
                int e = this.read();
                if (e == d) {    // Case must match. Ll is Welsh.
                    flags |= NumericValue.F_LONGLONG;
                    text.append((char) e);
                    d = this.read();
                } else {
                    flags |= NumericValue.F_LONG;
                    d = e;
                }
            } else if (d == 'I' || d == 'i') {
                if ((flags & NumericValue.FF_SIZE) != 0) {
                    this.warning("Multiple length suffixes after " + text);
                }
                flags |= NumericValue.F_INT;
                text.append((char) d);
                d = this.read();
            } else if (d == 'F' || d == 'f') {
                if ((flags & NumericValue.FF_SIZE) != 0) {
                    this.warning("Multiple length suffixes after " + text);
                }
                flags |= NumericValue.F_FLOAT;
                text.append((char) d);
                d = this.read();
            } else if (d == 'D' || d == 'd') {
                if ((flags & NumericValue.FF_SIZE) != 0) {
                    this.warning("Multiple length suffixes after " + text);
                }
                flags |= NumericValue.F_DOUBLE;
                text.append((char) d);
                d = this.read();
            } else if (Character.isUnicodeIdentifierPart(d)) {
                String reason = "Invalid suffix \"" + (char) d + "\" on numeric constant";
                // We've encountered something initially identified as a number.
                // Read in the rest of this token as an identifer but return it as an invalid.
                while (Character.isUnicodeIdentifierPart(d)) {
                    text.append((char) d);
                    d = this.read();
                }
                this.unread(d);
                return new Token(INVALID, text.toString(), reason);
            } else {
                this.unread(d);
                value.setFlags(flags);
                return new Token(NUMBER,
                        text.toString(), value);
            }
        }
    }

    /* Either a decimal part, or a hex exponent. */
    @NotNull
    private String _number_part(StringBuilder text, int base, boolean sign)
            throws IOException,
            LexerException {
        StringBuilder part = new StringBuilder();
        int d = this.read();
        if (sign && (d == '+' || d == '-')) {
            text.append((char) d);
            part.append((char) d);
            d = this.read();
        }
        while (Character.digit(d, base) != -1) {
            text.append((char) d);
            part.append((char) d);
            d = this.read();
        }
        this.unread(d);
        return part.toString();
    }

    /* We do not know whether know the first digit is valid. */
    @NotNull
    private Token number_hex(char x)
            throws IOException,
            LexerException {
        StringBuilder text = new StringBuilder("0");
        text.append(x);
        String integer = this._number_part(text, 16, false);
        NumericValue value = new NumericValue(16, integer);
        int d = this.read();
        if (d == '.') {
            text.append((char) d);
            String fraction = this._number_part(text, 16, false);
            value.setFractionalPart(fraction);
            d = this.read();
        }
        if (d == 'P' || d == 'p') {
            text.append((char) d);
            String exponent = this._number_part(text, 10, true);
            value.setExponent(2, exponent);
            d = this.read();
        }
        // XXX Make sure it's got enough parts
        return this._number_suffix(text, value, d);
    }

    private static boolean is_octal(@NotNull String text) {
        if (!text.startsWith("0")) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.digit(text.charAt(i), 8) == -1) {
                return false;
            }
        }
        return true;
    }

    /* We know we have at least one valid digit, but empty is not
     * fine. */
    @NotNull
    private Token number_decimal()
            throws IOException,
            LexerException {
        StringBuilder text = new StringBuilder();
        String integer = this._number_part(text, 10, false);
        String fraction = null;
        String exponent = null;
        int d = this.read();
        if (d == '.') {
            text.append((char) d);
            fraction = this._number_part(text, 10, false);
            d = this.read();
        }
        if (d == 'E' || d == 'e') {
            text.append((char) d);
            exponent = this._number_part(text, 10, true);
            d = this.read();
        }
        int base = 10;
        if (fraction == null && exponent == null && integer.startsWith("0")) {
            if (!is_octal(integer)) {
                this.warning("Decimal constant starts with 0, but not octal: " + integer);
            } else {
                base = 8;
            }
        }
        NumericValue value = new NumericValue(base, integer);
        if (fraction != null) {
            value.setFractionalPart(fraction);
        }
        if (exponent != null) {
            value.setExponent(10, exponent);
        }
        // XXX Make sure it's got enough parts
        return this._number_suffix(text, value, d);
    }

    /**
     * Section 6.4.4.1 of C99
     * <p>
     * (Not pasted here, but says that the initial negation is a separate token.)
     * <p>
     * Section 6.4.4.2 of C99
     * <p>
     * A floating constant has a significand part that may be followed
     * by an exponent part and a suffix that specifies its type. The
     * components of the significand part may include a digit sequence
     * representing the whole-number part, followed by a period (.),
     * followed by a digit sequence representing the fraction part.
     * <p>
     * The components of the exponent part are an e, E, p, or P
     * followed by an exponent consisting of an optionally signed digit
     * sequence. Either the whole-number part or the fraction part has to
     * be present; for decimal floating constants, either the period or
     * the exponent part has to be present.
     * <p>
     * The significand part is interpreted as a (decimal or hexadecimal)
     * rational number; the digit sequence in the exponent part is
     * interpreted as a decimal integer. For decimal floating constants,
     * the exponent indicates the power of 10 by which the significand
     * part is to be scaled. For hexadecimal floating constants, the
     * exponent indicates the power of 2 by which the significand part is
     * to be scaled.
     * <p>
     * For decimal floating constants, and also for hexadecimal
     * floating constants when FLT_RADIX is not a power of 2, the result
     * is either the nearest representable value, or the larger or smaller
     * representable value immediately adjacent to the nearest representable
     * value, chosen in an implementation-defined manner. For hexadecimal
     * floating constants when FLT_RADIX is a power of 2, the result is
     * correctly rounded.
     */
    @NotNull
    private Token number()
            throws IOException,
            LexerException {
        Token tok;
        int c = this.read();
        if (c == '0') {
            int d = this.read();
            if (d == 'x' || d == 'X') {
                tok = this.number_hex((char) d);
            } else {
                this.unread(d);
                this.unread(c);
                tok = this.number_decimal();
            }
        } else if (Character.isDigit(c) || c == '.') {
            this.unread(c);
            tok = this.number_decimal();
        } else {
            throw new LexerException("Asked to parse something as a number which isn't: " + (char) c);
        }
        return tok;
    }

    @NotNull
    private Token identifier(int c)
            throws IOException,
            LexerException {
        StringBuilder text = new StringBuilder();
        int d;
        text.append((char) c);
        for (; ; ) {
            d = this.read();
            if (Character.isIdentifierIgnorable(d))
                ;
            else if (Character.isJavaIdentifierPart(d)) {
                text.append((char) d);
            } else {
                break;
            }
        }
        this.unread(d);
        return new Token(IDENTIFIER, text.toString());
    }

    @NotNull
    private Token whitespace(int c)
            throws IOException,
            LexerException {
        StringBuilder text = new StringBuilder();
        int d;
        text.append((char) c);
        for (; ; ) {
            d = this.read();
            if (ppvalid && isLineSeparator(d)) /* XXX Ugly. */ {
                break;
            }
            if (Character.isWhitespace(d)) {
                text.append((char) d);
            } else {
                break;
            }
        }
        this.unread(d);
        return new Token(WHITESPACE, text.toString());
    }

    /* No token processed by cond() contains a newline. */
    @NotNull
    private Token cond(char c, int yes, int no)
            throws IOException,
            LexerException {
        int d = this.read();
        if (c == d) {
            return new Token(yes);
        }
        this.unread(d);
        return new Token(no);
    }

    @Override
    public Token token()
            throws IOException,
            LexerException {
        Token tok = null;

        int _l = line;
        int _c = column;

        int c = this.read();
        int d;

        switch (c) {
            case '\n':
                if (ppvalid) {
                    bol = true;
                    if (include) {
                        tok = new Token(NL, _l, _c, "\n");
                    } else {
                        int nls = 0;
                        do {
                            nls++;
                            d = this.read();
                        } while (d == '\n');
                        this.unread(d);
                        char[] text = new char[nls];
                        for (int i = 0; i < text.length; i++) {
                            text[i] = '\n';
                        }
                        // Skip the bol = false below.
                        tok = new Token(NL, _l, _c, new String(text));
                    }
                    return tok;
                }
                /* Let it be handled as whitespace. */
                break;

            case '!':
                tok = this.cond('=', NE, '!');
                break;

            case '#':
                if (bol) {
                    tok = new Token(HASH);
                } else {
                    tok = this.cond('#', PASTE, '#');
                }
                break;

            case '+':
                d = this.read();
                if (d == '+') {
                    tok = new Token(INC);
                } else if (d == '=') {
                    tok = new Token(PLUS_EQ);
                } else {
                    this.unread(d);
                }
                break;
            case '-':
                d = this.read();
                if (d == '-') {
                    tok = new Token(DEC);
                } else if (d == '=') {
                    tok = new Token(SUB_EQ);
                } else if (d == '>') {
                    tok = new Token(ARROW);
                } else {
                    this.unread(d);
                }
                break;

            case '*':
                tok = this.cond('=', MULT_EQ, '*');
                break;
            case '/':
                d = this.read();
                if (d == '*') {
                    tok = this.ccomment();
                } else if (d == '/') {
                    tok = this.cppcomment();
                } else if (d == '=') {
                    tok = new Token(DIV_EQ);
                } else {
                    this.unread(d);
                }
                break;

            case '%':
                d = this.read();
                if (d == '=') {
                    tok = new Token(MOD_EQ);
                } else if (digraphs && d == '>') {
                    tok = new Token('}');    // digraph
                } else if (digraphs && d == ':') {
                    PASTE:
                    {
                        d = this.read();
                        if (d != '%') {
                            this.unread(d);
                            tok = new Token('#');    // digraph
                            break PASTE;
                        }
                        d = this.read();
                        if (d != ':') {
                            this.unread(d);    // Unread 2 chars here.
                            this.unread('%');
                            tok = new Token('#');    // digraph
                            break PASTE;
                        }
                        tok = new Token(PASTE);    // digraph
                    }
                } else {
                    this.unread(d);
                }
                break;

            case ':':
                /* :: */
                d = this.read();
                if (digraphs && d == '>') {
                    tok = new Token(']');    // digraph
                } else {
                    this.unread(d);
                }
                break;

            case '<':
                if (include) {
                    tok = this.string('<', '>');
                } else {
                    d = this.read();
                    if (d == '=') {
                        tok = new Token(LE);
                    } else if (d == '<') {
                        tok = this.cond('=', LSH_EQ, LSH);
                    } else if (digraphs && d == ':') {
                        tok = new Token('[');    // digraph
                    } else if (digraphs && d == '%') {
                        tok = new Token('{');    // digraph
                    } else {
                        this.unread(d);
                    }
                }
                break;

            case '=':
                tok = this.cond('=', EQ, '=');
                break;

            case '>':
                d = this.read();
                if (d == '=') {
                    tok = new Token(GE);
                } else if (d == '>') {
                    tok = this.cond('=', RSH_EQ, RSH);
                } else {
                    this.unread(d);
                }
                break;

            case '^':
                tok = this.cond('=', XOR_EQ, '^');
                break;

            case '|':
                d = this.read();
                if (d == '=') {
                    tok = new Token(OR_EQ);
                } else if (d == '|') {
                    tok = this.cond('=', LOR_EQ, LOR);
                } else {
                    this.unread(d);
                }
                break;
            case '&':
                d = this.read();
                if (d == '&') {
                    tok = this.cond('=', LAND_EQ, LAND);
                } else if (d == '=') {
                    tok = new Token(AND_EQ);
                } else {
                    this.unread(d);
                }
                break;

            case '.':
                d = this.read();
                if (d == '.') {
                    tok = this.cond('.', ELLIPSIS, RANGE);
                } else {
                    this.unread(d);
                }
                if (Character.isDigit(d)) {
                    this.unread('.');
                    tok = this.number();
                }
                /* XXX decimal fraction */
                break;

            case '\'':
                tok = this.string('\'', '\'');
                break;

            case '"':
                tok = this.string('"', '"');
                break;

            case -1:
                this.close();
                tok = new Token(EOF, _l, _c, "<eof>");
                break;
        }

        if (tok == null) {
            if (Character.isWhitespace(c)) {
                tok = this.whitespace(c);
            } else if (Character.isDigit(c)) {
                this.unread(c);
                tok = this.number();
            } else if (Character.isJavaIdentifierStart(c)) {
                tok = this.identifier(c);
            } else {
                String text = TokenType.getTokenText(c);
                if (text == null) {
                    if ((c >>> 16) == 0)    // Character.isBmpCodePoint() is new in 1.7
                    {
                        text = Character.toString((char) c);
                    } else {
                        text = new String(Character.toChars(c));
                    }
                }
                tok = new Token(c, text);
            }
        }

        if (bol) {
            switch (tok.getType()) {
                case WHITESPACE:
                case CCOMMENT:
                    break;
                default:
                    bol = false;
                    break;
            }
        }

        tok.setLocation(_l, _c);
        return tok;
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
        super.close();
    }
}
