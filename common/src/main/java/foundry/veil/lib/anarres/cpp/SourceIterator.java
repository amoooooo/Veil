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
import java.util.Iterator;
import java.util.NoSuchElementException;

import static foundry.veil.lib.anarres.cpp.Token.EOF;

/*
 * NOTE: This File was edited by the Veil Team based on this commit: https://github.com/shevek/jcpp/commit/5e50e75ec33f5b4567cabfd60b6baca39524a8b7
 *
 * - Updated formatting to more closely follow project standards
 * - Removed all file/IO
 * - Fixed minor errors
 */

/**
 * An Iterator for {@link Source Sources},
 * returning {@link Token Tokens}.
 */
public class SourceIterator implements Iterator<Token> {

    private final Source source;
    private Token tok;

    public SourceIterator(@NotNull Source s) {
        this.source = s;
        this.tok = null;
    }

    /**
     * Rethrows IOException inside IllegalStateException.
     */
    private void advance() {
        try {
            if (tok == null) {
                tok = source.token();
            }
        } catch (LexerException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns true if the enclosed Source has more tokens.
     * <p>
     * The EOF token is never returned by the iterator.
     *
     * @throws IllegalStateException if the Source
     *                               throws a LexerException or IOException
     */
    @Override
    public boolean hasNext() {
        this.advance();
        return tok.getType() != EOF;
    }

    /**
     * Returns the next token from the enclosed Source.
     * <p>
     * The EOF token is never returned by the iterator.
     *
     * @throws IllegalStateException if the Source
     *                               throws a LexerException or IOException
     */
    @Override
    public Token next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        Token t = this.tok;
        this.tok = null;
        return t;
    }

    /**
     * Not supported.
     *
     * @throws UnsupportedOperationException unconditionally.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
