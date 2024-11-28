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

import org.jetbrains.annotations.NotNull;/*
 * NOTE: This File was edited by the Veil Team based on this commit: https://github.com/shevek/jcpp/commit/5e50e75ec33f5b4567cabfd60b6baca39524a8b7
 *
 * - Updated formatting to more closely follow project standards
 * - Removed all file/IO
 * - Fixed minor errors
 */

/**
 * A handler for preprocessor events, primarily errors and warnings.
 * <p>
 * If no PreprocessorListener is installed in a Preprocessor, all
 * error and warning events will throw an exception. Installing a
 * listener allows more intelligent handling of these events.
 */
public interface PreprocessorListener {

    /**
     * Handles a warning.
     * <p>
     * The behaviour of this method is defined by the
     * implementation. It may simply record the error message, or
     * it may throw an exception.
     */
    void handleWarning(@NotNull Source source, int line, int column,
                       @NotNull String msg)
            throws LexerException;

    /**
     * Handles an error.
     * <p>
     * The behaviour of this method is defined by the
     * implementation. It may simply record the error message, or
     * it may throw an exception.
     */
    void handleError(@NotNull Source source, int line, int column,
                     @NotNull String msg)
            throws LexerException;

    enum SourceChangeEvent {

        SUSPEND, PUSH, POP, RESUME
    }

    void handleSourceChange(@NotNull Source source, @NotNull SourceChangeEvent event);

}
