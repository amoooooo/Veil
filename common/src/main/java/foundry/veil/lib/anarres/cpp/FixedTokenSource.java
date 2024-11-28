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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/*
 * NOTE: This File was edited by the Veil Team based on this commit: https://github.com/shevek/jcpp/commit/5e50e75ec33f5b4567cabfd60b6baca39524a8b7
 *
 * - Updated formatting to more closely follow project standards
 * - Removed all file/IO
 * - Fixed minor errors
 */

/* pp */
class FixedTokenSource extends Source {

    private static final Token EOF = new Token(Token.EOF, "<ts-eof>");

    private final List<Token> tokens;
    private int idx;

    /* pp */ FixedTokenSource(Token... tokens) {
        this.tokens = Arrays.asList(tokens);
        this.idx = 0;
    }

    /* pp */ FixedTokenSource(List<Token> tokens) {
        this.tokens = tokens;
        this.idx = 0;
    }

    @Override
    public Token token()
            throws IOException,
            LexerException {
        if (idx >= tokens.size()) {
            return EOF;
        }
        return tokens.get(idx++);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("constant token stream ").append(tokens);
        Source parent = this.getParent();
        if (parent != null) {
            buf.append(" in ").append(parent);
        }
        return buf.toString();
    }
}
