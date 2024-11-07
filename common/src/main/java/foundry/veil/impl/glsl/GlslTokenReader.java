package foundry.veil.impl.glsl;

import org.jetbrains.annotations.Nullable;

public class GlslTokenReader {

    private final GlslLexer.Token[] tokens;
    private int cursor;

    public GlslTokenReader(GlslLexer.Token[] tokens) {
        this.tokens = tokens;
    }

    private int getCursorOffset() {
        int offset = -1;
        for (int i = 0; i <= Math.min(this.cursor, this.tokens.length - 1); i++) {
            offset += this.tokens[i].value().length() + 1;
        }
        return offset;
    }

    public String getString() {
        StringBuilder builder = new StringBuilder();
        for (GlslLexer.Token token : this.tokens) {
            builder.append(token.value()).append(' ');
        }
        return builder.toString().trim();
    }

    public boolean canRead(int length) {
        return this.cursor + length <= this.tokens.length;
    }

    public boolean canRead() {
        return this.canRead(1);
    }

    public @Nullable GlslLexer.Token peek() {
        return this.peek(0);
    }

    public @Nullable GlslLexer.Token peek(int amount) {
        return this.cursor + amount < this.tokens.length ? this.tokens[this.cursor + amount] : null;
    }

    public @Nullable GlslLexer.TokenType peekType(int amount) {
        return this.cursor + amount < this.tokens.length ? this.tokens[this.cursor + amount].type() : null;
    }

    public boolean canConsume(GlslLexer.TokenType token) {
        return this.canRead() && this.peek().type() == token;
    }

    public GlslLexer.Token consume(GlslLexer.TokenType token) throws GlslSyntaxException {
        if (!this.canRead() || this.peek().type() != token) {
            throw this.error("Expected " + token);
        }
        this.cursor++;
        return this.peek(-1);
    }

    public boolean tryConsume(GlslLexer.TokenType... tokens) {
        if (!this.canRead(tokens.length)) {
            return false;
        }

        for (int i = 0; i < tokens.length; i++) {
            if (this.peek(i).type() != tokens[i]) {
                return false;
            }
        }
        this.cursor += tokens.length;
        return true;
    }

    public GlslSyntaxException error(String error) {
        return new GlslSyntaxException(error, this.getString(), this.getCursorOffset());
    }

    public void skip() {
        this.cursor++;
    }

    public void skip(int amount) {
        this.cursor += amount;
    }

    public int getCursor() {
        return this.cursor;
    }

    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    @Override
    public String toString() {
        return "GlslTokenReader{cursor=" + this.cursor + ", token=" + this.peek() + "}";
    }
}
