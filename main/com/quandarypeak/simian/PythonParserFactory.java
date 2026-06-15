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

final class PythonParserFactory extends AbstractCFamilyParserFactory {

    // Python built-in types used in type annotations and isinstance() checks.
    // Promoted to TYPE so IGNORE_SUBTYPE_NAMES and IGNORE_VARIABLE_NAMES work correctly.
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            "bool", "bytearray", "bytes", "complex", "dict", "float", "frozenset",
            "int", "list", "memoryview", "object", "set", "str", "tuple", "type"
    ));

    // All Python 3 reserved keywords plus the Python 3.10+ soft keywords 'match' and 'case'.
    // These are promoted to KEYWORD so they survive IGNORE_IDENTIFIERS — structural words like
    // 'def', 'class', 'for', 'return', 'match', and 'case' must remain visible even when
    // user-defined names are collapsed to '_'.
    // 'match' and 'case' are contextual (soft) keywords — not in Python's keyword module —
    // but virtually no modern code uses them as variable names, so treating them as keywords
    // here is safe and produces correct fingerprints for pattern-matching code.
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "False", "None", "True",
            "and", "as", "assert", "async", "await",
            "break",
            "case", "class", "continue",
            "def", "del",
            "elif", "else", "except",
            "finally", "for", "from",
            "global",
            "if", "import", "in", "is",
            "lambda",
            "match",
            "nonlocal", "not",
            "or",
            "pass",
            "raise", "return",
            "try",
            "while", "with",
            "yield"
    ));

    // 'async' is the only declaration-level modifier in Python — suppressed when IGNORE_MODIFIERS
    // is active so that async and non-async versions of the same function compare as identical.
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList("async"));

    // 'import' triggers line suppression for both bare imports ("import os") and from-imports
    // ("from typing import List") since 'import' appears in both forms.  'from' is intentionally
    // omitted so that "raise X from e" lines are not accidentally suppressed.
    private static final Set<String> IMPORT_LINE_TRIGGERS = new HashSet<>(Arrays.asList("import"));

    static {
        KEYWORDS.addAll(TYPES);
    }

    @Override
    public final Parser createParser(final LineListener listener, final Options options) {
        TokenVisitor visitor = createBaseLanguageTokenVisitor(listener, options, MODIFIERS, IMPORT_LINE_TRIGGERS);
        visitor = new RecogniseIdentifiersTokenVisitor(visitor, TYPES, KEYWORDS);
        visitor = new StripPythonStringPrefixTokenVisitor(visitor);
        return new PythonParser(visitor);
    }
}
