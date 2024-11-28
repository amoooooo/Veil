/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package foundry.veil.lib.anarres.cpp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
 * NOTE: This File was edited by the Veil Team based on this commit: https://github.com/shevek/jcpp/commit/5e50e75ec33f5b4567cabfd60b6baca39524a8b7
 *
 * - Updated formatting to more closely follow project standards
 * - Removed all file/IO
 * - Fixed minor errors
 */

/**
 * @author shevek
 */
public enum PreprocessorCommand {

    PP_DEFINE("define"),
    PP_ELIF("elif"),
    PP_ELSE("else"),
    PP_ENDIF("endif"),
    PP_ERROR("error"),
    PP_IF("if"),
    PP_IFDEF("ifdef"),
    PP_IFNDEF("ifndef"),
    PP_INCLUDE("include"),
    PP_LINE("line"),
    PP_PRAGMA("pragma"),
    PP_UNDEF("undef"),
    PP_WARNING("warning"),
    PP_INCLUDE_NEXT("include_next"),
    PP_IMPORT("import");
    private final String text;

    /* pp */ PreprocessorCommand(String text) {
        this.text = text;
    }

    @Nullable
    public static PreprocessorCommand forText(@NotNull String text) {
        for (PreprocessorCommand ppcmd : PreprocessorCommand.values()) {
            if (ppcmd.text.equals(text)) {
                return ppcmd;
            }
        }
        return null;
    }
}
