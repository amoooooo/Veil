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

/*
 * NOTE: This File was edited by the Veil Team based on this commit: https://github.com/shevek/jcpp/commit/5e50e75ec33f5b4567cabfd60b6baca39524a8b7
 *
 * - Updated formatting to more closely follow project standards
 * - Removed all file/IO
 * - Fixed minor errors
 */

/**
 * Features of the Preprocessor, which may be enabled or disabled.
 */
public enum Feature {

    /**
     * Supports ANSI digraphs.
     */
    DIGRAPHS,
    /**
     * Supports ANSI trigraphs.
     */
    TRIGRAPHS,
    /**
     * Outputs linemarker tokens.
     */
    LINEMARKERS,
    /**
     * Reports tokens of type INVALID as errors.
     */
    CSYNTAX,
    /**
     * Preserves comments in the lexed output. Like cpp -C
     */
    KEEPCOMMENTS,
    /**
     * Preserves comments in the lexed output, even when inactive.
     */
    KEEPALLCOMMENTS,
    DEBUG,
    /**
     * Supports lexing of objective-C.
     */
    OBJCSYNTAX,
    INCLUDENEXT,
    /**
     * Random extensions.
     */
    PRAGMA_ONCE
}
