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

import foundry.veil.lib.anarres.cpp.PreprocessorListener.SourceChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

import static foundry.veil.lib.anarres.cpp.PreprocessorCommand.PP_ERROR;
import static foundry.veil.lib.anarres.cpp.Token.*;

/*
 * NOTE: This File was edited by the Veil Team based on this commit: https://github.com/shevek/jcpp/commit/5e50e75ec33f5b4567cabfd60b6baca39524a8b7
 *
 * - Updated formatting to more closely follow project standards
 * - Removed all file/IO
 * - Fixed minor errors
 */

/**
 * A C Preprocessor.
 * The Preprocessor outputs a token stream which does not need
 * re-lexing for C or C++. Alternatively, the output text may be
 * reconstructed by concatenating the {@link Token#getText() text}
 * values of the returned {@link Token Tokens}.
 */
/*
 * Source file name and line number information is conveyed by lines of the form
 *
 * # linenum filename flags
 *
 * These are called linemarkers. They are inserted as needed into
 * the output (but never within a string or character constant). They
 * mean that the following line originated in file filename at line
 * linenum. filename will never contain any non-printing characters;
 * they are replaced with octal escape sequences.
 *
 * After the file name comes zero or more flags, which are `1', `2',
 * `3', or `4'. If there are multiple flags, spaces separate them. Here
 * is what the flags mean:
 *
 * `1'
 * This indicates the start of a new file.
 * `2'
 * This indicates returning to a file (after having included another
 * file).
 * `3'
 * This indicates that the following text comes from a system header
 * file, so certain warnings should be suppressed.
 * `4'
 * This indicates that the following text should be treated as being
 * wrapped in an implicit extern "C" block.
 */
public class Preprocessor implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(Preprocessor.class);

    private static final Source INTERNAL = new Source() {
        @Override
        public Token token() throws LexerException {
            throw new LexerException("Cannot read from " + this.getName());
        }

        @Override
        public String getName() {
            return "internal data";
        }
    };
    private static final Macro __LINE__ = new Macro(INTERNAL, "__LINE__");
    private static final Macro __FILE__ = new Macro(INTERNAL, "__FILE__");
    private static final Macro __COUNTER__ = new Macro(INTERNAL, "__COUNTER__");

    private final List<Source> inputs;

    /* The fundamental engine. */
    private final Map<String, Macro> macros;
    private final Stack<State> states;
    private Source source;

    /* Miscellaneous support. */
    private int counter;
    private final Set<String> onceseenpaths = new HashSet<String>();

    /* Support junk to make it work like cpp */
    private List<String> quoteincludepath;    /* -iquote */

    private List<String> sysincludepath;        /* -I */

    private List<String> frameworkspath;
    private final Set<Feature> features;
    private final Set<Warning> warnings;
    private PreprocessorListener listener;

    public Preprocessor() {
        this.inputs = new ArrayList<Source>();

        this.macros = new HashMap<String, Macro>();
        macros.put(__LINE__.getName(), __LINE__);
        macros.put(__FILE__.getName(), __FILE__);
        macros.put(__COUNTER__.getName(), __COUNTER__);
        this.states = new Stack<State>();
        states.push(new State());
        this.source = null;

        this.counter = 0;

        this.quoteincludepath = new ArrayList<String>();
        this.sysincludepath = new ArrayList<String>();
        this.frameworkspath = new ArrayList<String>();
        this.features = EnumSet.noneOf(Feature.class);
        this.warnings = EnumSet.noneOf(Warning.class);
        this.listener = null;
    }

    public Preprocessor(@NotNull Source initial) {
        this();
        this.addInput(initial);
    }

    /**
     * Sets the PreprocessorListener which handles events for
     * this Preprocessor.
     * <p>
     * The listener is notified of warnings, errors and source
     * changes, amongst other things.
     */
    public void setListener(@NotNull PreprocessorListener listener) {
        this.listener = listener;
        Source s = source;
        while (s != null) {
            // s.setListener(listener);
            s.init(this);
            s = s.getParent();
        }
    }

    /**
     * Returns the PreprocessorListener which handles events for
     * this Preprocessor.
     */
    @NotNull
    public PreprocessorListener getListener() {
        return listener;
    }

    /**
     * Returns the feature-set for this Preprocessor.
     * <p>
     * This set may be freely modified by user code.
     */
    @NotNull
    public Set<Feature> getFeatures() {
        return features;
    }

    /**
     * Adds a feature to the feature-set of this Preprocessor.
     */
    public void addFeature(@NotNull Feature f) {
        features.add(f);
    }

    /**
     * Adds features to the feature-set of this Preprocessor.
     */
    public void addFeatures(@NotNull Collection<Feature> f) {
        features.addAll(f);
    }

    /**
     * Adds features to the feature-set of this Preprocessor.
     */
    public void addFeatures(Feature... f) {
        this.addFeatures(Arrays.asList(f));
    }

    /**
     * Returns true if the given feature is in
     * the feature-set of this Preprocessor.
     */
    public boolean getFeature(@NotNull Feature f) {
        return features.contains(f);
    }

    /**
     * Returns the warning-set for this Preprocessor.
     * <p>
     * This set may be freely modified by user code.
     */
    @NotNull
    public Set<Warning> getWarnings() {
        return warnings;
    }

    /**
     * Adds a warning to the warning-set of this Preprocessor.
     */
    public void addWarning(@NotNull Warning w) {
        warnings.add(w);
    }

    /**
     * Adds warnings to the warning-set of this Preprocessor.
     */
    public void addWarnings(@NotNull Collection<Warning> w) {
        warnings.addAll(w);
    }

    /**
     * Returns true if the given warning is in
     * the warning-set of this Preprocessor.
     */
    public boolean getWarning(@NotNull Warning w) {
        return warnings.contains(w);
    }

    /**
     * Adds input for the Preprocessor.
     * <p>
     * Inputs are processed in the order in which they are added.
     */
    public void addInput(@NotNull Source source) {
        source.init(this);
        inputs.add(source);
    }

    /**
     * Handles an error.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * error. Otherwise, an exception is thrown.
     */
    protected void error(int line, int column, @NotNull String msg)
            throws LexerException {
        if (listener != null) {
            listener.handleError(source, line, column, msg);
        } else {
            throw new LexerException("Error at " + line + ":" + column + ": " + msg);
        }
    }

    /**
     * Handles an error.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * error. Otherwise, an exception is thrown.
     *
     * @see #error(int, int, String)
     */
    protected void error(@NotNull Token tok, @NotNull String msg)
            throws LexerException {
        this.error(tok.getLine(), tok.getColumn(), msg);
    }

    /**
     * Handles a warning.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * warning. Otherwise, an exception is thrown.
     */
    protected void warning(int line, int column, @NotNull String msg)
            throws LexerException {
        if (warnings.contains(Warning.ERROR)) {
            this.error(line, column, msg);
        } else if (listener != null) {
            listener.handleWarning(source, line, column, msg);
        } else {
            throw new LexerException("Warning at " + line + ":" + column + ": " + msg);
        }
    }

    /**
     * Handles a warning.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * warning. Otherwise, an exception is thrown.
     *
     * @see #warning(int, int, String)
     */
    protected void warning(@NotNull Token tok, @NotNull String msg)
            throws LexerException {
        this.warning(tok.getLine(), tok.getColumn(), msg);
    }

    /**
     * Adds a Macro to this Preprocessor.
     * <p>
     * The given {@link Macro} object encapsulates both the name
     * and the expansion.
     *
     * @throws LexerException if the definition fails or is otherwise illegal.
     */
    public void addMacro(@NotNull Macro m) throws LexerException {
        // System.out.println("Macro " + m);
        String name = m.getName();
        /* Already handled as a source error in macro(). */
        if ("defined".equals(name)) {
            throw new LexerException("Cannot redefine name 'defined'");
        }
        macros.put(m.getName(), m);
    }

    /**
     * Defines the given name as a macro.
     * <p>
     * The String value is lexed into a token stream, which is
     * used as the macro expansion.
     *
     * @throws LexerException if the definition fails or is otherwise illegal.
     */
    public void addMacro(@NotNull String name, @NotNull String value)
            throws LexerException {
        try {
            Macro m = new Macro(name);
            StringLexerSource s = new StringLexerSource(value);
            for (; ; ) {
                Token tok = s.token();
                if (tok.getType() == EOF) {
                    break;
                }
                m.addToken(tok);
            }
            this.addMacro(m);
        } catch (IOException e) {
            throw new LexerException(e);
        }
    }

    /**
     * Defines the given name as a macro, with the value <code>1</code>.
     * <p>
     * This is a convnience method, and is equivalent to
     * <code>addMacro(name, "1")</code>.
     *
     * @throws LexerException if the definition fails or is otherwise illegal.
     */
    public void addMacro(@NotNull String name)
            throws LexerException {
        this.addMacro(name, "1");
    }

    /**
     * Sets the user include path used by this Preprocessor.
     */
    /* Note for future: Create an IncludeHandler? */
    public void setQuoteIncludePath(@NotNull List<String> path) {
        this.quoteincludepath = path;
    }

    /**
     * Returns the user include-path of this Preprocessor.
     * <p>
     * This list may be freely modified by user code.
     */
    @NotNull
    public List<String> getQuoteIncludePath() {
        return quoteincludepath;
    }

    /**
     * Sets the system include path used by this Preprocessor.
     */
    /* Note for future: Create an IncludeHandler? */
    public void setSystemIncludePath(@NotNull List<String> path) {
        this.sysincludepath = path;
    }

    /**
     * Returns the system include-path of this Preprocessor.
     * <p>
     * This list may be freely modified by user code.
     */
    @NotNull
    public List<String> getSystemIncludePath() {
        return sysincludepath;
    }

    /**
     * Sets the Objective-C frameworks path used by this Preprocessor.
     */
    /* Note for future: Create an IncludeHandler? */
    public void setFrameworksPath(@NotNull List<String> path) {
        this.frameworkspath = path;
    }

    /**
     * Returns the Objective-C frameworks path used by this
     * Preprocessor.
     * <p>
     * This list may be freely modified by user code.
     */
    @NotNull
    public List<String> getFrameworksPath() {
        return frameworkspath;
    }

    /**
     * Returns the Map of Macros parsed during the run of this
     * Preprocessor.
     *
     * @return The {@link Map} of macros currently defined.
     */
    @NotNull
    public Map<String, Macro> getMacros() {
        return macros;
    }

    /**
     * Returns the named macro.
     * <p>
     * While you can modify the returned object, unexpected things
     * might happen if you do.
     *
     * @return the Macro object, or null if not found.
     */
    @Nullable
    public Macro getMacro(@NotNull String name) {
        return macros.get(name);
    }

    /* States */
    private void push_state() {
        State top = states.peek();
        states.push(new State(top));
    }

    private void pop_state()
            throws LexerException {
        State s = states.pop();
        if (states.isEmpty()) {
            this.error(0, 0, "#" + "endif without #" + "if");
            states.push(s);
        }
    }

    private boolean isActive() {
        State state = states.peek();
        return state.isParentActive() && state.isActive();
    }


    /* Sources */

    /**
     * Returns the top Source on the input stack.
     *
     * @return the top Source on the input stack.
     * @see Source
     * @see #push_source(Source, boolean)
     * @see #pop_source()
     */
    // @Nullable
    protected Source getSource() {
        return source;
    }

    /**
     * Pushes a Source onto the input stack.
     *
     * @param source  the new Source to push onto the top of the input stack.
     * @param autopop if true, the Source is automatically removed from the input stack at EOF.
     * @see #getSource()
     * @see #pop_source()
     */
    protected void push_source(@NotNull Source source, boolean autopop) {
        source.init(this);
        source.setParent(this.source, autopop);
        // source.setListener(listener);
        if (listener != null) {
            listener.handleSourceChange(this.source, SourceChangeEvent.SUSPEND);
        }
        this.source = source;
        if (listener != null) {
            listener.handleSourceChange(this.source, SourceChangeEvent.PUSH);
        }
    }

    /**
     * Pops a Source from the input stack.
     *
     * @param linemarker TODO: currently ignored, might be a bug?
     * @throws IOException if an I/O error occurs.
     * @see #getSource()
     * @see #push_source(Source, boolean)
     */
    @Nullable
    protected Token pop_source(boolean linemarker)
            throws IOException {
        if (listener != null) {
            listener.handleSourceChange(this.source, SourceChangeEvent.POP);
        }
        Source s = this.source;
        this.source = s.getParent();
        /* Always a noop unless called externally. */
        s.close();
        if (listener != null && this.source != null) {
            listener.handleSourceChange(this.source, SourceChangeEvent.RESUME);
        }

        Source t = this.getSource();
        if (this.getFeature(Feature.LINEMARKERS)
                && s.isNumbered()
                && t != null) {
            /* We actually want 'did the nested source
             * contain a newline token', which isNumbered()
             * approximates. This is not perfect, but works. */
            return this.line_token(t.getLine(), t.getName(), " 2");
        }

        return null;
    }

    protected void pop_source()
            throws IOException {
        this.pop_source(false);
    }

    @NotNull
    private Token next_source() {
        if (inputs.isEmpty()) {
            return new Token(EOF);
        }
        Source s = inputs.remove(0);
        this.push_source(s, true);
        return this.line_token(s.getLine(), s.getName(), " 1");
    }

    /* Source tokens */
    private Token source_token;

    /* XXX Make this include the NL, and make all cpp directives eat
     * their own NL. */
    @NotNull
    private Token line_token(int line, @Nullable String name, @NotNull String extra) {
        StringBuilder buf = new StringBuilder();
        buf.append("#line ").append(line)
                .append(" \"");
        /* XXX This call to escape(name) is correct but ugly. */
        if (name == null) {
            buf.append("<no file>");
        } else {
            MacroTokenSource.escape(buf, name);
        }
        buf.append("\"").append(extra).append("\n");
        return new Token(P_LINE, line, 0, buf.toString(), null);
    }

    @NotNull
    private Token source_token()
            throws IOException,
            LexerException {
        if (source_token != null) {
            Token tok = source_token;
            source_token = null;
            if (this.getFeature(Feature.DEBUG)) {
                LOG.debug("Returning unget token " + tok);
            }
            return tok;
        }

        for (; ; ) {
            Source s = this.getSource();
            if (s == null) {
                Token t = this.next_source();
                if (t.getType() == P_LINE && !this.getFeature(Feature.LINEMARKERS)) {
                    continue;
                }
                return t;
            }
            Token tok = s.token();
            /* XXX Refactor with skipline() */
            if (tok.getType() == EOF && s.isAutopop()) {
                // System.out.println("Autopop " + s);
                Token mark = this.pop_source(true);
                if (mark != null) {
                    return mark;
                }
                continue;
            }
            if (this.getFeature(Feature.DEBUG)) {
                LOG.debug("Returning fresh token " + tok);
            }
            return tok;
        }
    }

    private void source_untoken(Token tok) {
        if (this.source_token != null) {
            throw new IllegalStateException("Cannot return two tokens");
        }
        this.source_token = tok;
    }

    private boolean isWhite(Token tok) {
        int type = tok.getType();
        return (type == WHITESPACE)
                || (type == CCOMMENT)
                || (type == CPPCOMMENT);
    }

    private Token source_token_nonwhite()
            throws IOException,
            LexerException {
        Token tok;
        do {
            tok = this.source_token();
        } while (this.isWhite(tok));
        return tok;
    }

    /**
     * Returns an NL or an EOF token.
     * <p>
     * The metadata on the token will be correct, which is better
     * than generating a new one.
     * <p>
     * This method can, as of recent patches, return a P_LINE token.
     */
    private Token source_skipline(boolean white)
            throws IOException,
            LexerException {
        // (new Exception("skipping line")).printStackTrace(System.out);
        Source s = this.getSource();
        Token tok = s.skipline(white);
        /* XXX Refactor with source_token() */
        if (tok.getType() == EOF && s.isAutopop()) {
            // System.out.println("Autopop " + s);
            Token mark = this.pop_source(true);
            if (mark != null) {
                return mark;
            }
        }
        return tok;
    }

    /* processes and expands a macro. */
    private boolean macro(Macro m, Token orig)
            throws IOException,
            LexerException {
        Token tok;
        List<Argument> args;

        // System.out.println("pp: expanding " + m);
        if (m.isFunctionLike()) {
            OPEN:
            for (; ; ) {
                tok = this.source_token();
                // System.out.println("pp: open: token is " + tok);
                switch (tok.getType()) {
                    case WHITESPACE:    /* XXX Really? */

                    case CCOMMENT:
                    case CPPCOMMENT:
                    case NL:
                        break;    /* continue */

                    case '(':
                        break OPEN;
                    default:
                        this.source_untoken(tok);
                        return false;
                }
            }

            // tok = expanded_token_nonwhite();
            tok = this.source_token_nonwhite();

            /* We either have, or we should have args.
             * This deals elegantly with the case that we have
             * one empty arg. */
            if (tok.getType() != ')' || m.getArgs() > 0) {
                args = new ArrayList<Argument>();

                Argument arg = new Argument();
                int depth = 0;
                boolean space = false;

                ARGS:
                for (; ; ) {
                    // System.out.println("pp: arg: token is " + tok);
                    switch (tok.getType()) {
                        case EOF:
                            this.error(tok, "EOF in macro args");
                            return false;

                        case ',':
                            if (depth == 0) {
                                if (m.isVariadic()
                                        && /* We are building the last arg. */ args.size() == m.getArgs() - 1) {
                                    /* Just add the comma. */
                                    arg.addToken(tok);
                                } else {
                                    args.add(arg);
                                    arg = new Argument();
                                }
                            } else {
                                arg.addToken(tok);
                            }
                            space = false;
                            break;
                        case ')':
                            if (depth == 0) {
                                args.add(arg);
                                break ARGS;
                            } else {
                                depth--;
                                arg.addToken(tok);
                            }
                            space = false;
                            break;
                        case '(':
                            depth++;
                            arg.addToken(tok);
                            space = false;
                            break;

                        case WHITESPACE:
                        case CCOMMENT:
                        case CPPCOMMENT:
                        case NL:
                            /* Avoid duplicating spaces. */
                            space = true;
                            break;

                        default:
                            /* Do not put space on the beginning of
                             * an argument token. */
                            if (space && !arg.isEmpty()) {
                                arg.addToken(Token.space);
                            }
                            arg.addToken(tok);
                            space = false;
                            break;

                    }
                    // tok = expanded_token();
                    tok = this.source_token();
                }
                /* space may still be true here, thus trailing space
                 * is stripped from arguments. */

                if (args.size() != m.getArgs()) {
                    if (m.isVariadic()) {
                        if (args.size() == m.getArgs() - 1) {
                            args.add(new Argument());
                        } else {
                            this.error(tok,
                                    "variadic macro " + m.getName()
                                            + " has at least " + (m.getArgs() - 1) + " parameters "
                                            + "but given " + args.size() + " args");
                            return false;
                        }
                    } else {
                        this.error(tok,
                                "macro " + m.getName()
                                        + " has " + m.getArgs() + " parameters "
                                        + "but given " + args.size() + " args");
                        /* We could replay the arg tokens, but I
                         * note that GNU cpp does exactly what we do,
                         * i.e. output the macro name and chew the args.
                         */
                        return false;
                    }
                }

                for (Argument a : args) {
                    a.expand(this);
                }

                // System.out.println("Macro " + m + " args " + args);
            } else {
                /* nargs == 0 and we (correctly) got () */
                args = null;
            }

        } else {
            /* Macro without args. */
            args = null;
        }

        if (m == __LINE__) {
            this.push_source(new FixedTokenSource(
                    new Token(NUMBER,
                            orig.getLine(), orig.getColumn(),
                            Integer.toString(orig.getLine()),
                            new NumericValue(10, Integer.toString(orig.getLine())))), true);
        } else if (m == __FILE__) {
            StringBuilder buf = new StringBuilder("\"");
            String name = this.getSource().getName();
            if (name == null) {
                name = "<no file>";
            }
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                switch (c) {
                    case '\\':
                        buf.append("\\\\");
                        break;
                    case '"':
                        buf.append("\\\"");
                        break;
                    default:
                        buf.append(c);
                        break;
                }
            }
            buf.append("\"");
            String text = buf.toString();
            this.push_source(new FixedTokenSource(
                    new Token(STRING,
                            orig.getLine(), orig.getColumn(),
                            text, text)), true);
        } else if (m == __COUNTER__) {
            /* This could equivalently have been done by adding
             * a special Macro subclass which overrides getTokens(). */
            int value = this.counter++;
            this.push_source(new FixedTokenSource(
                    new Token(NUMBER,
                            orig.getLine(), orig.getColumn(),
                            Integer.toString(value),
                            new NumericValue(10, Integer.toString(value)))), true);
        } else {
            this.push_source(new MacroTokenSource(m, args), true);
        }

        return true;
    }

    /**
     * Expands an argument.
     */
    /* I'd rather this were done lazily, but doing so breaks spec. */
    @NotNull
    /* pp */ List<Token> expand(@NotNull List<Token> arg)
            throws IOException,
            LexerException {
        List<Token> expansion = new ArrayList<Token>();
        boolean space = false;

        this.push_source(new FixedTokenSource(arg), false);

        EXPANSION:
        for (; ; ) {
            Token tok = this.expanded_token();
            switch (tok.getType()) {
                case EOF:
                    break EXPANSION;

                case WHITESPACE:
                case CCOMMENT:
                case CPPCOMMENT:
                    space = true;
                    break;

                default:
                    if (space && !expansion.isEmpty()) {
                        expansion.add(Token.space);
                    }
                    expansion.add(tok);
                    space = false;
                    break;
            }
        }

        // Always returns null.
        this.pop_source(false);

        return expansion;
    }

    /* processes a #define directive */
    private Token define()
            throws IOException,
            LexerException {
        Token tok = this.source_token_nonwhite();
        if (tok.getType() != IDENTIFIER) {
            this.error(tok, "Expected identifier");
            return this.source_skipline(false);
        }
        /* if predefined */

        String name = tok.getText();
        if ("defined".equals(name)) {
            this.error(tok, "Cannot redefine name 'defined'");
            return this.source_skipline(false);
        }

        Macro m = new Macro(this.getSource(), name);
        List<String> args;

        tok = this.source_token();
        if (tok.getType() == '(') {
            tok = this.source_token_nonwhite();
            if (tok.getType() != ')') {
                args = new ArrayList<String>();
                ARGS:
                for (; ; ) {
                    switch (tok.getType()) {
                        case IDENTIFIER:
                            args.add(tok.getText());
                            break;
                        case ELLIPSIS:
                            // Unnamed Variadic macro
                            args.add("__VA_ARGS__");
                            // We just named the ellipsis, but we unget the token
                            // to allow the ELLIPSIS handling below to process it.
                            this.source_untoken(tok);
                            break;
                        case NL:
                        case EOF:
                            this.error(tok,
                                    "Unterminated macro parameter list");
                            return tok;
                        default:
                            this.error(tok,
                                    "error in macro parameters: "
                                            + tok.getText());
                            return this.source_skipline(false);
                    }
                    tok = this.source_token_nonwhite();
                    switch (tok.getType()) {
                        case ',':
                            break;
                        case ELLIPSIS:
                            tok = this.source_token_nonwhite();
                            if (tok.getType() != ')') {
                                this.error(tok,
                                        "ellipsis must be on last argument");
                            }
                            m.setVariadic(true);
                            break ARGS;
                        case ')':
                            break ARGS;

                        case NL:
                        case EOF:
                            /* Do not skip line. */
                            this.error(tok,
                                    "Unterminated macro parameters");
                            return tok;
                        default:
                            this.error(tok,
                                    "Bad token in macro parameters: "
                                            + tok.getText());
                            return this.source_skipline(false);
                    }
                    tok = this.source_token_nonwhite();
                }
            } else {
                assert tok.getType() == ')' : "Expected ')'";
                args = Collections.emptyList();
            }

            m.setArgs(args);
        } else {
            /* For searching. */
            args = Collections.emptyList();
            this.source_untoken(tok);
        }

        /* Get an expansion for the macro, using indexOf. */
        boolean space = false;
        boolean paste = false;
        int idx;

        /* Ensure no space at start. */
        tok = this.source_token_nonwhite();
        EXPANSION:
        for (; ; ) {
            switch (tok.getType()) {
                case EOF:
                    break EXPANSION;
                case NL:
                    break EXPANSION;

                case CCOMMENT:
                case CPPCOMMENT:
                    /* XXX This is where we implement GNU's cpp -CC. */
                    // break;
                case WHITESPACE:
                    if (!paste) {
                        space = true;
                    }
                    break;

                /* Paste. */
                case PASTE:
                    space = false;
                    paste = true;
                    m.addPaste(new Token(M_PASTE,
                            tok.getLine(), tok.getColumn(),
                            "#" + "#", null));
                    break;

                /* Stringify. */
                case '#':
                    if (space) {
                        m.addToken(Token.space);
                    }
                    space = false;
                    Token la = this.source_token_nonwhite();
                    if (la.getType() == IDENTIFIER
                            && ((idx = args.indexOf(la.getText())) != -1)) {
                        m.addToken(new Token(M_STRING,
                                la.getLine(), la.getColumn(),
                                "#" + la.getText(),
                                Integer.valueOf(idx)));
                    } else {
                        m.addToken(tok);
                        /* Allow for special processing. */
                        this.source_untoken(la);
                    }
                    break;

                case IDENTIFIER:
                    if (space) {
                        m.addToken(Token.space);
                    }
                    space = false;
                    paste = false;
                    idx = args.indexOf(tok.getText());
                    if (idx == -1) {
                        m.addToken(tok);
                    } else {
                        m.addToken(new Token(M_ARG,
                                tok.getLine(), tok.getColumn(),
                                tok.getText(),
                                Integer.valueOf(idx)));
                    }
                    break;

                default:
                    if (space) {
                        m.addToken(Token.space);
                    }
                    space = false;
                    paste = false;
                    m.addToken(tok);
                    break;
            }
            tok = this.source_token();
        }

        if (this.getFeature(Feature.DEBUG)) {
            LOG.debug("Defined macro " + m);
        }
        this.addMacro(m);

        return tok;    /* NL or EOF. */

    }

    @NotNull
    private Token undef()
            throws IOException,
            LexerException {
        Token tok = this.source_token_nonwhite();
        if (tok.getType() != IDENTIFIER) {
            this.error(tok,
                    "Expected identifier, not " + tok.getText());
            if (tok.getType() == NL || tok.getType() == EOF) {
                return tok;
            }
        } else {
            Macro m = this.getMacro(tok.getText());
            if (m != null) {
                /* XXX error if predefined */
                macros.remove(m.getName());
            }
        }
        return this.source_skipline(true);
    }

    @NotNull
    private Token include(boolean next)
            throws IOException,
            LexerException {
        LexerSource lexer = (LexerSource) source;
        try {
            lexer.setInclude(true);
            Token tok = this.token_nonwhite();

            String name;
            boolean quoted;

            if (tok.getType() == STRING) {
                /* XXX Use the original text, not the value.
                 * Backslashes must not be treated as escapes here. */
                StringBuilder buf = new StringBuilder((String) tok.getValue());
                HEADER:
                for (; ; ) {
                    tok = this.token_nonwhite();
                    switch (tok.getType()) {
                        case STRING:
                            buf.append((String) tok.getValue());
                            break;
                        case NL:
                        case EOF:
                            break HEADER;
                        default:
                            this.warning(tok,
                                    "Unexpected token on #" + "include line");
                            return this.source_skipline(false);
                    }
                }
                name = buf.toString();
                quoted = true;
            } else if (tok.getType() == HEADER) {
                name = (String) tok.getValue();
                quoted = false;
                tok = this.source_skipline(true);
            } else {
                this.error(tok,
                        "Expected string or header, not " + tok.getText());
                return switch (tok.getType()) {
                    case NL, EOF -> tok;
                    default ->
                        /* Only if not a NL or EOF already. */
                            this.source_skipline(false);
                };
            }

            /* 'tok' is the 'nl' after the include. We use it after the
             * #line directive. */
            if (this.getFeature(Feature.LINEMARKERS)) {
                return this.line_token(1, source.getName(), " 1");
            }
            return tok;
        } finally {
            lexer.setInclude(false);
        }
    }

    protected void pragma(@NotNull Token name, @NotNull List<Token> value)
            throws IOException,
            LexerException {
        if (this.getFeature(Feature.PRAGMA_ONCE)) {
            if ("once".equals(name.getText())) {
                return;
            }
        }
        this.warning(name, "Unknown #" + "pragma: " + name.getText());
    }

    @NotNull
    private Token pragma()
            throws IOException,
            LexerException {
        Token name;

        NAME:
        for (; ; ) {
            Token tok = this.source_token();
            switch (tok.getType()) {
                case EOF:
                    /* There ought to be a newline before EOF.
                     * At least, in any skipline context. */
                    /* XXX Are we sure about this? */
                    this.warning(tok,
                            "End of file in #" + "pragma");
                    return tok;
                case NL:
                    /* This may contain one or more newlines. */
                    this.warning(tok,
                            "Empty #" + "pragma");
                    return tok;
                case CCOMMENT:
                case CPPCOMMENT:
                case WHITESPACE:
                    continue NAME;
                case IDENTIFIER:
                    name = tok;
                    break NAME;
                default:
                    this.warning(tok,
                            "Illegal #" + "pragma " + tok.getText());
                    return this.source_skipline(false);
            }
        }

        Token tok;
        List<Token> value = new ArrayList<Token>();
        VALUE:
        for (; ; ) {
            tok = this.source_token();
            switch (tok.getType()) {
                case EOF:
                    /* There ought to be a newline before EOF.
                     * At least, in any skipline context. */
                    /* XXX Are we sure about this? */
                    this.warning(tok,
                            "End of file in #" + "pragma");
                    break VALUE;
                case NL:
                    /* This may contain one or more newlines. */
                    break VALUE;
                case CCOMMENT:
                case CPPCOMMENT:
                    break;
                default:
                    value.add(tok);
                    break;
            }
        }

        this.pragma(name, value);

        return tok;    /* The NL. */

    }

    /* For #error and #warning. */
    private void error(@NotNull Token pptok, boolean is_error)
            throws IOException,
            LexerException {
        StringBuilder buf = new StringBuilder();
        buf.append('#').append(pptok.getText()).append(' ');
        /* Peculiar construction to ditch first whitespace. */
        Token tok = this.source_token_nonwhite();
        ERROR:
        for (; ; ) {
            switch (tok.getType()) {
                case NL:
                case EOF:
                    break ERROR;
                default:
                    buf.append(tok.getText());
                    break;
            }
            tok = this.source_token();
        }
        if (is_error) {
            this.error(pptok, buf.toString());
        } else {
            this.warning(pptok, buf.toString());
        }
    }

    /* This bypasses token() for #elif expressions.
     * If we don't do this, then isActive() == false
     * causes token() to simply chew the entire input line. */
    @NotNull
    private Token expanded_token()
            throws IOException,
            LexerException {
        for (; ; ) {
            Token tok = this.source_token();
            // System.out.println("Source token is " + tok);
            if (tok.getType() == IDENTIFIER) {
                Macro m = this.getMacro(tok.getText());
                if (m == null) {
                    return tok;
                }
                if (source.isExpanding(m)) {
                    return tok;
                }
                if (this.macro(m, tok)) {
                    continue;
                }
            }
            return tok;
        }
    }

    @NotNull
    private Token expanded_token_nonwhite()
            throws IOException,
            LexerException {
        Token tok;
        do {
            tok = this.expanded_token();
            // System.out.println("expanded token is " + tok);
        } while (this.isWhite(tok));
        return tok;
    }

    @Nullable
    private Token expr_token = null;

    @NotNull
    private Token expr_token()
            throws IOException,
            LexerException {
        Token tok = expr_token;

        if (tok != null) {
            // System.out.println("ungetting");
            expr_token = null;
        } else {
            tok = this.expanded_token_nonwhite();
            // System.out.println("expt is " + tok);

            if (tok.getType() == IDENTIFIER
                    && tok.getText().equals("defined")) {
                Token la = this.source_token_nonwhite();
                boolean paren = false;
                if (la.getType() == '(') {
                    paren = true;
                    la = this.source_token_nonwhite();
                }

                // System.out.println("Core token is " + la);
                if (la.getType() != IDENTIFIER) {
                    this.error(la,
                            "defined() needs identifier, not "
                                    + la.getText());
                    tok = new Token(NUMBER,
                            la.getLine(), la.getColumn(),
                            "0", new NumericValue(10, "0"));
                } else if (macros.containsKey(la.getText())) {
                    // System.out.println("Found macro");
                    tok = new Token(NUMBER,
                            la.getLine(), la.getColumn(),
                            "1", new NumericValue(10, "1"));
                } else {
                    // System.out.println("Not found macro");
                    tok = new Token(NUMBER,
                            la.getLine(), la.getColumn(),
                            "0", new NumericValue(10, "0"));
                }

                if (paren) {
                    la = this.source_token_nonwhite();
                    if (la.getType() != ')') {
                        this.expr_untoken(la);
                        this.error(la, "Missing ) in defined(). Got " + la.getText());
                    }
                }
            }
        }

        // System.out.println("expr_token returns " + tok);
        return tok;
    }

    private void expr_untoken(@NotNull Token tok)
            throws LexerException {
        if (expr_token != null) {
            throw new InternalException(
                    "Cannot unget two expression tokens."
            );
        }
        expr_token = tok;
    }

    private int expr_priority(@NotNull Token op) {
        switch (op.getType()) {
            case '/':
                return 11;
            case '%':
                return 11;
            case '*':
                return 11;
            case '+':
                return 10;
            case '-':
                return 10;
            case LSH:
                return 9;
            case RSH:
                return 9;
            case '<':
                return 8;
            case '>':
                return 8;
            case LE:
                return 8;
            case GE:
                return 8;
            case EQ:
                return 7;
            case NE:
                return 7;
            case '&':
                return 6;
            case '^':
                return 5;
            case '|':
                return 4;
            case LAND:
                return 3;
            case LOR:
                return 2;
            case '?':
                return 1;
            default:
                // System.out.println("Unrecognised operator " + op);
                return 0;
        }
    }

    private int expr_char(Token token) {
        Object value = token.getValue();
        if (value instanceof Character) {
            return ((Character) value).charValue();
        }
        String text = String.valueOf(value);
        if (text.length() == 0) {
            return 0;
        }
        return text.charAt(0);
    }

    private long expr(int priority)
            throws IOException,
            LexerException {
        /*
         * (new Exception("expr(" + priority + ") called")).printStackTrace();
         */

        Token tok = this.expr_token();
        long lhs, rhs;

        // System.out.println("Expr lhs token is " + tok);
        switch (tok.getType()) {
            case '(':
                lhs = this.expr(0);
                tok = this.expr_token();
                if (tok.getType() != ')') {
                    this.expr_untoken(tok);
                    this.error(tok, "Missing ) in expression. Got " + tok.getText());
                    return 0;
                }
                break;

            case '~':
                lhs = ~this.expr(11);
                break;
            case '!':
                lhs = this.expr(11) == 0 ? 1 : 0;
                break;
            case '-':
                lhs = -this.expr(11);
                break;
            case NUMBER:
                NumericValue value = (NumericValue) tok.getValue();
                lhs = value.longValue();
                break;
            case CHARACTER:
                lhs = this.expr_char(tok);
                break;
            case IDENTIFIER:
                if (warnings.contains(Warning.UNDEF)) {
                    this.warning(tok, "Undefined token '" + tok.getText()
                            + "' encountered in conditional.");
                }
                lhs = 0;
                break;

            default:
                this.expr_untoken(tok);
                this.error(tok,
                        "Bad token in expression: " + tok.getText());
                return 0;
        }

        for (; ; ) {
            // System.out.println("expr: lhs is " + lhs + ", pri = " + priority);
            Token op = this.expr_token();
            int pri = this.expr_priority(op);    /* 0 if not a binop. */

            if (pri == 0 || priority >= pri) {
                this.expr_untoken(op);
                break;
            }
            rhs = this.expr(pri);
            // System.out.println("rhs token is " + rhs);
            switch (op.getType()) {
                case '/':
                    if (rhs == 0) {
                        this.error(op, "Division by zero");
                        lhs = 0;
                    } else {
                        lhs = lhs / rhs;
                    }
                    break;
                case '%':
                    if (rhs == 0) {
                        this.error(op, "Modulus by zero");
                        lhs = 0;
                    } else {
                        lhs = lhs % rhs;
                    }
                    break;
                case '*':
                    lhs = lhs * rhs;
                    break;
                case '+':
                    lhs = lhs + rhs;
                    break;
                case '-':
                    lhs = lhs - rhs;
                    break;
                case '<':
                    lhs = lhs < rhs ? 1 : 0;
                    break;
                case '>':
                    lhs = lhs > rhs ? 1 : 0;
                    break;
                case '&':
                    lhs = lhs & rhs;
                    break;
                case '^':
                    lhs = lhs ^ rhs;
                    break;
                case '|':
                    lhs = lhs | rhs;
                    break;

                case LSH:
                    lhs = lhs << rhs;
                    break;
                case RSH:
                    lhs = lhs >> rhs;
                    break;
                case LE:
                    lhs = lhs <= rhs ? 1 : 0;
                    break;
                case GE:
                    lhs = lhs >= rhs ? 1 : 0;
                    break;
                case EQ:
                    lhs = lhs == rhs ? 1 : 0;
                    break;
                case NE:
                    lhs = lhs != rhs ? 1 : 0;
                    break;
                case LAND:
                    lhs = (lhs != 0) && (rhs != 0) ? 1 : 0;
                    break;
                case LOR:
                    lhs = (lhs != 0) || (rhs != 0) ? 1 : 0;
                    break;

                case '?': {
                    tok = this.expr_token();
                    if (tok.getType() != ':') {
                        this.expr_untoken(tok);
                        this.error(tok, "Missing : in conditional expression. Got " + tok.getText());
                        return 0;
                    }
                    long falseResult = this.expr(0);
                    lhs = (lhs != 0) ? rhs : falseResult;
                }
                break;

                default:
                    this.error(op,
                            "Unexpected operator " + op.getText());
                    return 0;

            }
        }

        /*
         * (new Exception("expr returning " + lhs)).printStackTrace();
         */
        // System.out.println("expr returning " + lhs);
        return lhs;
    }

    @NotNull
    private Token toWhitespace(@NotNull Token tok) {
        String text = tok.getText();
        int len = text.length();
        boolean cr = false;
        int nls = 0;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);

            switch (c) {
                case '\r':
                    cr = true;
                    nls++;
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
                    nls++;
                    break;
            }
        }

        char[] cbuf = new char[nls];
        Arrays.fill(cbuf, '\n');
        return new Token(WHITESPACE,
                tok.getLine(), tok.getColumn(),
                new String(cbuf));
    }

    @NotNull
    private Token _token()
            throws IOException,
            LexerException {

        for (; ; ) {
            Token tok;
            if (!this.isActive()) {
                Source s = this.getSource();
                if (s == null) {
                    Token t = this.next_source();
                    if (t.getType() == P_LINE && !this.getFeature(Feature.LINEMARKERS)) {
                        continue;
                    }
                    return t;
                }

                try {
                    /* XXX Tell lexer to ignore warnings. */
                    s.setActive(false);
                    tok = this.source_token();
                } finally {
                    /* XXX Tell lexer to stop ignoring warnings. */
                    s.setActive(true);
                }
                switch (tok.getType()) {
                    case HASH:
                    case NL:
                    case EOF:
                        /* The preprocessor has to take action here. */
                        break;
                    case WHITESPACE:
                        return tok;
                    case CCOMMENT:
                    case CPPCOMMENT:
                        // Patch up to preserve whitespace.
                        if (this.getFeature(Feature.KEEPALLCOMMENTS)) {
                            return tok;
                        }
                        if (!this.isActive()) {
                            return this.toWhitespace(tok);
                        }
                        if (this.getFeature(Feature.KEEPCOMMENTS)) {
                            return tok;
                        }
                        return this.toWhitespace(tok);
                    default:
                        // Return NL to preserve whitespace.
                        /* XXX This might lose a comment. */
                        return this.source_skipline(false);
                }
            } else {
                tok = this.source_token();
            }

            LEX:
            switch (tok.getType()) {
                case EOF:
                    /* Pop the stacks. */
                    return tok;

                case WHITESPACE:
                case NL:
                    return tok;

                case CCOMMENT:
                case CPPCOMMENT:
                    return tok;

                case '!':
                case '%':
                case '&':
                case '(':
                case ')':
                case '*':
                case '+':
                case ',':
                case '-':
                case '/':
                case ':':
                case ';':
                case '<':
                case '=':
                case '>':
                case '?':
                case '[':
                case ']':
                case '^':
                case '{':
                case '|':
                case '}':
                case '~':
                case '.':

                    /* From Olivier Chafik for Objective C? */
                case '@':
                    /* The one remaining ASCII, might as well. */
                case '`':

                    // case '#':
                case AND_EQ:
                case ARROW:
                case CHARACTER:
                case DEC:
                case DIV_EQ:
                case ELLIPSIS:
                case EQ:
                case GE:
                case HEADER:    /* Should only arise from include() */

                case INC:
                case LAND:
                case LE:
                case LOR:
                case LSH:
                case LSH_EQ:
                case SUB_EQ:
                case MOD_EQ:
                case MULT_EQ:
                case NE:
                case OR_EQ:
                case PLUS_EQ:
                case RANGE:
                case RSH:
                case RSH_EQ:
                case STRING:
                case SQSTRING:
                case XOR_EQ:
                    return tok;

                case NUMBER:
                    return tok;

                case IDENTIFIER:
                    Macro m = this.getMacro(tok.getText());
                    if (m == null) {
                        return tok;
                    }
                    if (source.isExpanding(m)) {
                        return tok;
                    }
                    if (this.macro(m, tok)) {
                        break;
                    }
                    return tok;

                case P_LINE:
                    if (this.getFeature(Feature.LINEMARKERS)) {
                        return tok;
                    }
                    break;

                case INVALID:
                    if (this.getFeature(Feature.CSYNTAX)) {
                        this.error(tok, String.valueOf(tok.getValue()));
                    }
                    return tok;

                default:
                    throw new InternalException("Bad token " + tok);
                    // break;

                case HASH:
                    tok = this.source_token_nonwhite();
                    // (new Exception("here")).printStackTrace();
                    switch (tok.getType()) {
                        case NL:
                            break LEX;    /* Some code has #\n */

                        case IDENTIFIER:
                            break;
                        default:
                            this.error(tok,
                                    "Preprocessor directive not a word "
                                            + tok.getText());
                            return this.source_skipline(false);
                    }
                    PreprocessorCommand ppcmd = PreprocessorCommand.forText(tok.getText());
                    if (ppcmd == null) {
                        this.error(tok,
                                "Unknown preprocessor directive "
                                        + tok.getText());
                        return this.source_skipline(false);
                    }

                    switch (ppcmd) {

                        case PP_DEFINE:
                            if (!this.isActive()) {
                                return this.source_skipline(false);
                            } else {
                                return this.define();
                            }
                            // break;

                        case PP_UNDEF:
                            if (!this.isActive()) {
                                return this.source_skipline(false);
                            } else {
                                return this.undef();
                            }
                            // break;

                        case PP_INCLUDE:
                            if (!this.isActive()) {
                                return this.source_skipline(false);
                            } else {
                                return this.include(false);
                            }
                            // break;
                        case PP_INCLUDE_NEXT:
                            if (!this.isActive()) {
                                return this.source_skipline(false);
                            }
                            if (!this.getFeature(Feature.INCLUDENEXT)) {
                                this.error(tok,
                                        "Directive include_next not enabled"
                                );
                                return this.source_skipline(false);
                            }
                            return this.include(true);
                        // break;

                        case PP_WARNING:
                        case PP_ERROR:
                            if (!this.isActive()) {
                                return this.source_skipline(false);
                            } else {
                                this.error(tok, ppcmd == PP_ERROR);
                            }
                            break;

                        case PP_IF:
                            this.push_state();
                            if (!this.isActive()) {
                                return this.source_skipline(false);
                            }
                            expr_token = null;
                            states.peek().setActive(this.expr(0) != 0);
                            tok = this.expr_token();    /* unget */

                            if (tok.getType() == NL) {
                                return tok;
                            }
                            return this.source_skipline(true);
                        // break;

                        case PP_ELIF:
                            State state = states.peek();
                            if (false) {
                                /* Check for 'if' */
                            } else if (state.sawElse()) {
                                this.error(tok,
                                        "#elif after #" + "else");
                                return this.source_skipline(false);
                            } else if (!state.isParentActive()) {
                                /* Nested in skipped 'if' */
                                return this.source_skipline(false);
                            } else if (state.isActive()) {
                                /* The 'if' part got executed. */
                                state.setParentActive(false);
                                /* This is like # else # if but with
                                 * only one # end. */
                                state.setActive(false);
                                return this.source_skipline(false);
                            } else {
                                expr_token = null;
                                state.setActive(this.expr(0) != 0);
                                tok = this.expr_token();    /* unget */

                                if (tok.getType() == NL) {
                                    return tok;
                                }
                                return this.source_skipline(true);
                            }
                            // break;

                        case PP_ELSE:
                            state = states.peek();
                            if (false)
                                /* Check for 'if' */ ;
                            else if (state.sawElse()) {
                                this.error(tok,
                                        "#" + "else after #" + "else");
                                return this.source_skipline(false);
                            } else {
                                state.setSawElse();
                                state.setActive(!state.isActive());
                                return this.source_skipline(warnings.contains(Warning.ENDIF_LABELS));
                            }
                            // break;

                        case PP_IFDEF:
                            this.push_state();
                            if (!this.isActive()) {
                                return this.source_skipline(false);
                            } else {
                                tok = this.source_token_nonwhite();
                                // System.out.println("ifdef " + tok);
                                if (tok.getType() != IDENTIFIER) {
                                    this.error(tok,
                                            "Expected identifier, not "
                                                    + tok.getText());
                                    return this.source_skipline(false);
                                } else {
                                    String text = tok.getText();
                                    boolean exists
                                            = macros.containsKey(text);
                                    states.peek().setActive(exists);
                                    return this.source_skipline(true);
                                }
                            }
                            // break;

                        case PP_IFNDEF:
                            this.push_state();
                            if (!this.isActive()) {
                                return this.source_skipline(false);
                            } else {
                                tok = this.source_token_nonwhite();
                                if (tok.getType() != IDENTIFIER) {
                                    this.error(tok,
                                            "Expected identifier, not "
                                                    + tok.getText());
                                    return this.source_skipline(false);
                                } else {
                                    String text = tok.getText();
                                    boolean exists
                                            = macros.containsKey(text);
                                    states.peek().setActive(!exists);
                                    return this.source_skipline(true);
                                }
                            }
                            // break;

                        case PP_ENDIF:
                            this.pop_state();
                            return this.source_skipline(warnings.contains(Warning.ENDIF_LABELS));
                        // break;

                        case PP_LINE:
                            return this.source_skipline(false);
                        // break;

                        case PP_PRAGMA:
                            if (!this.isActive()) {
                                return this.source_skipline(false);
                            }
                            return this.pragma();
                        // break;

                        default:
                            /* Actual unknown directives are
                             * processed above. If we get here,
                             * we succeeded the map lookup but
                             * failed to handle it. Therefore,
                             * this is (unconditionally?) fatal. */
                            // if (isActive()) /* XXX Could be warning. */
                            throw new InternalException(
                                    "Internal error: Unknown directive "
                                            + tok);
                            // return source_skipline(false);
                    }

            }
        }
    }

    @NotNull
    private Token token_nonwhite()
            throws IOException,
            LexerException {
        Token tok;
        do {
            tok = this._token();
        } while (this.isWhite(tok));
        return tok;
    }

    /**
     * Returns the next preprocessor token.
     *
     * @return The next fully preprocessed token.
     * @throws IOException       if an I/O error occurs.
     * @throws LexerException    if a preprocessing error occurs.
     * @throws InternalException if an unexpected error condition arises.
     * @see Token
     */
    @NotNull
    public Token token()
            throws IOException,
            LexerException {
        Token tok = this._token();
        if (this.getFeature(Feature.DEBUG)) {
            LOG.debug("pp: Returning " + tok);
        }
        return tok;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        Source s = this.getSource();
        while (s != null) {
            buf.append(" -> ").append(s).append("\n");
            s = s.getParent();
        }

        Map<String, Macro> macros = new TreeMap<String, Macro>(this.getMacros());
        for (Macro macro : macros.values()) {
            buf.append("#").append("macro ").append(macro).append("\n");
        }

        return buf.toString();
    }

    @Override
    public void close()
            throws IOException {
        {
            Source s = source;
            while (s != null) {
                s.close();
                s = s.getParent();
            }
        }
        for (Source s : inputs) {
            s.close();
        }
    }

}
