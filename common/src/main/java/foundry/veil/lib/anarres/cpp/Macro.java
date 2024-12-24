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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
 * NOTE: This File was edited by the Veil Team based on this commit: https://github.com/shevek/jcpp/commit/5e50e75ec33f5b4567cabfd60b6baca39524a8b7
 *
 * - Updated formatting to more closely follow project standards
 * - Removed all file/IO
 * - Fixed minor errors
 */

/**
 * A macro object.
 * <p>
 * This encapsulates a name, an argument count, and a token stream
 * for replacement. The replacement token stream may contain the
 * extra tokens {@link Token#M_ARG} and {@link Token#M_STRING}.
 */
public class Macro {

    private Source source;
    private final String name;
    /* It's an explicit decision to keep these around here. We don't
     * need to; the argument token type is M_ARG and the value
     * is the index. The strings themselves are only used in
     * stringification of the macro, for debugging. */
    private List<String> args;
    private boolean variadic;
    private final List<Token> tokens;

    public Macro(Source source, String name) {
        this.source = source;
        this.name = name;
        this.args = null;
        this.variadic = false;
        this.tokens = new ArrayList<>();
    }

    public Macro(String name) {
        this(null, name);
    }

    /**
     * Sets the Source from which this macro was parsed.
     */
    public void setSource(Source s) {
        this.source = s;
    }

    /**
     * Returns the Source from which this macro was parsed.
     * <p>
     * This method may return null if the macro was not parsed
     * from a regular file.
     */
    public Source getSource() {
        return this.source;
    }

    /**
     * Returns the name of this macro.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the arguments to this macro.
     */
    public void setArgs(List<String> args) {
        this.args = args;
    }

    /**
     * Returns true if this is a function-like macro.
     */
    public boolean isFunctionLike() {
        return this.args != null;
    }

    /**
     * Returns the number of arguments to this macro.
     */
    public int getArgs() {
        return this.args.size();
    }

    /**
     * Sets the variadic flag on this Macro.
     */
    public void setVariadic(boolean b) {
        this.variadic = b;
    }

    /**
     * Returns true if this is a variadic function-like macro.
     */
    public boolean isVariadic() {
        return this.variadic;
    }

    /**
     * Adds a token to the expansion of this macro.
     */
    public void addToken(Token tok) {
        this.tokens.add(tok);
    }

    /**
     * Adds a "paste" operator to the expansion of this macro.
     * <p>
     * A paste operator causes the next token added to be pasted
     * to the previous token when the macro is expanded.
     * It is an error for a macro to end with a paste token.
     */
    public void addPaste(Token tok) {
        /*
         * Given: tok0 ## tok1
         * We generate: M_PASTE, tok0, tok1
         * This extends as per a stack language:
         * tok0 ## tok1 ## tok2 ->
         *   M_PASTE, tok0, M_PASTE, tok1, tok2
         */
        this.tokens.add(this.tokens.size() - 1, tok);
    }

    /* pp */ List<Token> getTokens() {
        return this.tokens;
    }

    /* Paste tokens are inserted before the first of the two pasted
     * tokens, so it's a kind of bytecode notation. This method
     * swaps them around again. We know that there will never be two
     * sequential paste tokens, so a boolean is sufficient. */
    public String getText() {
        StringBuilder buf = new StringBuilder();
        boolean paste = false;
        for (Token tok : this.tokens) {
            if (tok.getType() == Token.M_PASTE) {
                assert !paste : "Two sequential pastes.";
                paste = true;
                continue;
            } else {
                buf.append(tok.getText());
            }
            if (paste) {
                buf.append(" #" + "# ");
                paste = false;
            }
            // buf.append(tokens.get(i));
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(this.name);
        if (this.args != null) {
            buf.append('(');
            Iterator<String> it = this.args.iterator();
            while (it.hasNext()) {
                buf.append(it.next());
                if (it.hasNext()) {
                    buf.append(", ");
                } else if (this.isVariadic()) {
                    buf.append("...");
                }
            }
            buf.append(')');
        }
        if (!this.tokens.isEmpty()) {
            buf.append(" => ").append(this.getText());
        }
        return buf.toString();
    }

}
