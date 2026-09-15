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

public final class TypeScriptParserFactory extends AbstractCFamilyParserFactory {

    // TypeScript primitive and special types promoted to TYPE so that
    // IGNORE_SUBTYPE_NAMES and IGNORE_VARIABLE_NAMES work correctly on them.
    // These are also added to KEYWORDS (via the static initialiser below) so that
    // they survive IGNORE_IDENTIFIERS.
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            "any", "bigint", "boolean", "never", "number", "object",
            "string", "symbol", "unknown", "void"
    ));

    // All TypeScript / JavaScript structural keywords.  Promoted to KEYWORD so they survive
    // IGNORE_IDENTIFIERS — control-flow words (if, for, return…) and declaration words
    // (class, function, const…) must remain visible even when user-defined names are erased.
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            // Control flow
            "break", "case", "catch", "continue", "debugger", "default",
            "delete", "do", "else", "finally", "for", "if", "in",
            "instanceof", "new", "of", "return", "super", "switch",
            "this", "throw", "try", "typeof", "while", "with", "yield",
            // Async
            "async", "await",
            // Declaration keywords (structural — preserved so code shape survives IGNORE_IDENTIFIERS)
            "class", "const", "enum", "export", "extends", "function",
            "implements", "import", "interface", "let", "namespace", "module",
            "static", "var",
            // TypeScript-specific structural keywords
            "abstract", "as", "asserts", "declare", "from", "global",
            "infer", "is", "keyof", "override", "readonly", "satisfies",
            "type", "unique",
            // Literal keywords
            "false", "null", "true", "undefined"
    ));

    // Words stripped when IGNORE_MODIFIERS is active.  Includes visibility modifiers,
    // declaration keywords, and TypeScript-specific qualifiers that are often stylistic
    // rather than structural — so that e.g. an async function and its sync counterpart,
    // or a public method and a private one, compare as identical when the option is set.
    // Note: 'await' is intentionally absent — it is an expression operator ('await fetch()')
    // and stripping it would make awaited and non-awaited calls fingerprint-identical.
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList(
            "abstract", "accessor", "async",
            "class", "const", "declare", "enum", "export", "extends", "final",
            "function", "implements", "interface", "native", "namespace", "module",
            "override", "private", "protected", "public",
            "readonly", "satisfies", "static", "throws", "transient",
            "tuple", "type", "var", "let", "volatile"
    ));

    // 'import' triggers line suppression for import statements.  'package' (present in
    // the JavaScript factory) is intentionally omitted — TypeScript has no package
    // declarations, and suppressing a line just because it contains the word 'package'
    // would be incorrect for TypeScript source.
    private static final Set<String> IMPORT_LINE_TRIGGERS = new HashSet<>(Arrays.asList("import"));

    static {
        // Ensure TYPES entries survive IGNORE_IDENTIFIERS (which only preserves KEYWORD type).
        KEYWORDS.addAll(TYPES);
    }

    @Override
    public Parser createParser(final LineListener listener, final Options options) {
        TokenVisitor visitor = createBaseLanguageTokenVisitor(listener, options, MODIFIERS, IMPORT_LINE_TRIGGERS);
        if (options.hasOption(Option.IGNORE_TYPE_ANNOTATIONS)) {
            visitor = new IgnoreTypeAnnotationsTokenVisitor(visitor);
        }
        visitor = new RecogniseIdentifiersTokenVisitor(visitor, TYPES, KEYWORDS);
        return new TypeScriptParser(visitor);
    }
}
