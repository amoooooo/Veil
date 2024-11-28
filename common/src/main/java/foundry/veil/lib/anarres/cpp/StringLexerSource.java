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

import java.io.StringReader;

/*
 * NOTE: This File was edited by the Veil Team based on this commit: https://github.com/shevek/jcpp/commit/5e50e75ec33f5b4567cabfd60b6baca39524a8b7
 *
 * - Updated formatting to more closely follow project standards
 * - Removed all file/IO
 * - Fixed minor errors
 */

/**
 * A Source for lexing a String.
 * <p>
 * This class is used by token pasting, but can be used by user
 * code.
 */
public class StringLexerSource extends LexerSource {

    /**
     * Creates a new Source for lexing the given String.
     *
     * @param string  The input string to lex.
     * @param ppvalid true if preprocessor directives are to be
     *                honoured within the string.
     */
    public StringLexerSource(String string, boolean ppvalid) {
        super(new StringReader(string), ppvalid);
    }

    /**
     * Creates a new Source for lexing the given String.
     * <p>
     * Equivalent to calling <code>new StringLexerSource(string, false)</code>.
     * <p>
     * By default, preprocessor directives are not honoured within
     * the string.
     *
     * @param string The input string to lex.
     */
    public StringLexerSource(String string) {
        this(string, false);
    }

    @Override
    public String toString() {
        return "string literal";
    }
}
