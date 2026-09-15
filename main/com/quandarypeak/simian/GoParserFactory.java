/*
 * Copyright 2022-2026 Quandary Peak Research, Inc.
 * Original authorship by Simon Harris.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quandarypeak.simian;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class GoParserFactory extends AbstractCFamilyParserFactory {

    // Go built-in types — all lowercase, so require explicit set membership to be
    // classified as TYPE (they would otherwise fall through to VARIABLE via isTypeName).
    // Added to KEYWORDS via static initialiser so they survive IGNORE_IDENTIFIERS.
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            "any", "bool", "byte", "comparable", "complex64", "complex128",
            "error", "float32", "float64", "int", "int8", "int16", "int32", "int64",
            "rune", "string", "uint", "uint8", "uint16", "uint32", "uint64", "uintptr"
    ));

    // All 25 Go reserved keywords, built-in functions, and literal keywords.
    // Promoted to KEYWORD so they survive IGNORE_IDENTIFIERS — control-flow words
    // (if, for, return, …) and structural words (func, chan, struct, …) must remain
    // visible when user-defined names are erased.
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            // The 25 reserved keywords (Go specification)
            "break", "case", "chan", "const", "continue", "default", "defer",
            "else", "fallthrough", "for", "func", "go", "goto", "if", "import",
            "interface", "map", "package", "range", "return", "select", "struct",
            "switch", "type", "var",
            // Built-in functions
            "append", "cap", "clear", "close", "copy", "delete", "len", "make",
            "max", "min", "new", "panic", "print", "println", "recover",
            // Literal keywords
            "false", "iota", "nil", "true"
    ));

    // Words stripped when IGNORE_MODIFIERS is active.
    // 'const'/'var' normalize declaration style (like TypeScript's const/let).
    // 'func'/'type' normalize declaration vs usage context.
    // 'defer' normalises scheduling: 'defer f()' and 'f()' are structurally the same
    // call site and differ only in when they execute.
    // 'go' (goroutine launch) is intentionally excluded — 'go f()' changes execution
    // semantics, not just declaration style (analogous to 'await' in TypeScript which
    // is also excluded from MODIFIERS for the same reason).
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList(
            "const", "defer", "func", "type", "var"
    ));

    // 'package' and 'import' trigger unconditional line suppression via
    // IgnoreLinesTokenVisitor — package declarations and import statements are
    // boilerplate that should never contribute to duplicate matches.
    private static final Set<String> IMPORT_LINE_TRIGGERS = new HashSet<>(Arrays.asList(
            "import", "package"
    ));

    static {
        // TYPES entries must survive IGNORE_IDENTIFIERS (which only preserves KEYWORD type).
        KEYWORDS.addAll(TYPES);
        // MODIFIERS and IMPORT_LINE_TRIGGERS entries must survive IGNORE_IDENTIFIERS too.
        KEYWORDS.addAll(MODIFIERS);
        KEYWORDS.addAll(IMPORT_LINE_TRIGGERS);
    }

    @Override
    public Parser createParser(final LineListener listener, final Options options) {
        final TokenVisitor visitor = createBaseLanguageTokenVisitor(listener, options, MODIFIERS, IMPORT_LINE_TRIGGERS);
        return new GoParser(new RecogniseIdentifiersTokenVisitor(visitor, TYPES, KEYWORDS));
    }
}
