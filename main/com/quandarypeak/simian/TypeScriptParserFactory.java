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

final class TypeScriptParserFactory extends AbstractCFamilyParserFactory {

    // TypeScript primitive and special types promoted to TYPE so that
    // IGNORE_SUBTYPE_NAMES and IGNORE_VARIABLE_NAMES work correctly on them.
    // Note: they do NOT survive IGNORE_IDENTIFIERS as a result - RecogniseIdentifiersTokenVisitor
    // checks TYPES membership before KEYWORDS membership, so a TYPES entry is always classified
    // TYPE, never KEYWORD, and IgnoreIdentifiersTokenVisitor only preserves KEYWORD-typed tokens.
    // Under IGNORE_IDENTIFIERS these are erased to '_' the same as a user-defined type name.
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
            "infer", "is", "keyof", "out", "override", "readonly", "satisfies",
            "type", "unique",
            // Accessor keywords (get/set are contextual - not classified as TYPE, KEYWORD,
            // or CONSTANT by RecogniseIdentifiersTokenVisitor otherwise, so they fall into
            // the pending variable/method name slot and get silently overwritten by the
            // accessor name that immediately follows, e.g. 'get celsius' loses 'get')
            "get", "set",
            // Literal keywords
            "false", "null", "true", "undefined"
    ));

    // Contextual keywords: TypeScript-invented or JS-contextual words that are NOT reserved
    // by the language grammar outside one specific syntactic position - they are ordinary,
    // legal method/property names everywhere else (e.g. 'cache.get(key)', 'Array.from(arr)',
    // 'Array.of(1, 2, 3)'). Unlike true reserved words (class, return, ...), unconditionally
    // classifying these as KEYWORD is wrong when they're used as an ordinary identifier.
    // RecogniseAccessorKeywordTokenVisitor disambiguates using a single rule: genuine keyword
    // role only when NOT immediately preceded by '.' AND immediately followed by another
    // identifier (not '(' directly) - see its class doc for worked examples.
    // 'async' and 'accessor' are deliberately excluded: 'async () => {}' (async arrow
    // functions) is genuine keyword usage immediately followed by '(' - the exact signature
    // this visitor otherwise treats as "ordinary identifier being called/declared" - so the
    // single followed-by-identifier-vs-'(' rule misclassifies it. 'accessor' isn't in
    // KEYWORDS at all (only MODIFIERS), so it never reaches this visitor pre-classified the
    // way the other words do; including it would need KEYWORDS membership too. Both need a
    // more careful fix than this pass covers.
    private static final Set<String> CONTEXTUAL_KEYWORDS = new HashSet<>(Arrays.asList(
            "get", "set", "from", "of", "is", "as",
            "declare", "namespace", "module", "abstract", "override", "readonly", "out", "satisfies"
    ));

    // Words stripped when IGNORE_MODIFIERS is active.  Includes visibility modifiers,
    // declaration keywords, and TypeScript-specific qualifiers that are often stylistic
    // rather than structural — so that e.g. an async function and its sync counterpart,
    // or a public method and a private one, compare as identical when the option is set.
    // Note: 'await' is intentionally absent — it is an expression operator ('await fetch()')
    // and stripping it would make awaited and non-awaited calls fingerprint-identical.
    // 'get'/'set'/'declare'/'namespace'/'module'/'abstract'/'override'/'readonly'/'out'/
    // 'satisfies' are deliberately absent - RecogniseAccessorKeywordTokenVisitor now owns
    // their stripping decision (IgnoreWordsTokenVisitor matches by name only, which would
    // incorrectly strip an ordinary method/property literally sharing one of these names too,
    // e.g. 'cache.get(key)', 'obj.satisfies(x)'). 'accessor'/'async' stay here unchanged -
    // see the comment on CONTEXTUAL_KEYWORDS for why they're excluded from that mechanism.
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList(
            "accessor", "async",
            "class", "const", "enum", "export", "extends",
            "function", "implements", "interface",
            "private", "protected", "public",
            "static",
            "type", "var", "let"
    ));

    // 'import' triggers line suppression for import statements.  'package' (present in
    // the JavaScript factory) is intentionally omitted — TypeScript has no package
    // declarations, and suppressing a line just because it contains the word 'package'
    // would be incorrect for TypeScript source.
    private static final Set<String> IMPORT_LINE_TRIGGERS = new HashSet<>(Arrays.asList("import"));

    static {
        // Note: this has no observable effect on IGNORE_IDENTIFIERS - see the comment on
        // TYPES above. Kept for parity with PythonParserFactory/CParserFactory, which have
        // the same pattern.
        KEYWORDS.addAll(TYPES);
    }

    @Override
    public Parser createParser(final LineListener listener, final Options options) {
        TokenVisitor visitor = createBaseLanguageTokenVisitor(listener, options, MODIFIERS, IMPORT_LINE_TRIGGERS);
        if (options.hasOption(Option.IGNORE_TYPE_ANNOTATIONS)) {
            visitor = new IgnoreTypeAnnotationsTokenVisitor(visitor);
        }
        visitor = new RecogniseAccessorKeywordTokenVisitor(visitor, CONTEXTUAL_KEYWORDS, options.hasOption(Option.IGNORE_MODIFIERS));
        visitor = new RecogniseIdentifiersTokenVisitor(visitor, TYPES, KEYWORDS);
        return new JavaScriptParser(visitor);
    }
}
