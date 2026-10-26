/*
 * Copyright 2022-2026 Quandary Peak Research, Inc.
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

import java.util.Objects;
import java.util.Set;

/**
 * Disambiguates TypeScript/JavaScript contextual keywords (e.g. {@code get}, {@code set},
 * {@code from}, {@code of}, {@code is}, {@code as}) from ordinary identifiers that happen to
 * share the same spelling.
 *
 * <p>Unlike true reserved words (e.g. {@code class}, {@code return}), these words are not
 * forbidden as identifiers by the language grammar - they are only meaningful in a specific
 * syntactic position, and are completely ordinary method/property names everywhere else
 * (e.g. {@code cache.get(key)}, {@code Array.from(arr)}, {@code Array.of(1, 2, 3)}).
 * {@link RecogniseIdentifiersTokenVisitor} has no such positional awareness - it classifies
 * every occurrence of a word in its {@code KEYWORDS} set as {@code KEYWORD}, unconditionally.
 *
 * <p>This visitor sits immediately downstream of {@link RecogniseIdentifiersTokenVisitor} (for
 * TypeScript) or as the outermost wrap (for JavaScript, which has no classification step at
 * all) and corrects the classification using a single rule: a word in {@code contextualKeywords}
 * is treated as its genuine contextual-keyword role only when it is (a) not immediately
 * preceded by {@code '.'} and (b) immediately followed by another identifier (not {@code '('}
 * directly). Otherwise it is an ordinary identifier - reclassified as {@code METHOD} (if
 * immediately followed by {@code '('}) or {@code VARIABLE} (otherwise - a property reference,
 * not a call) so it is correctly erased under {@code IGNORE_IDENTIFIERS}/{@code
 * IGNORE_VARIABLE_NAMES} and never stripped under {@code IGNORE_MODIFIERS}, exactly like any
 * other method/variable name.
 *
 * <p>Examples resolved correctly by this single rule: {@code get fahrenheit()} (keyword role)
 * vs {@code cache.get(key)} and {@code class Foo { get() {} }} (ordinary); {@code x as string}
 * vs {@code obj.as(x)}; {@code for (a of b)} vs {@code Array.of(x)}; {@code x is string} vs
 * {@code obj.is(x)}.
 */
final class RecogniseAccessorKeywordTokenVisitor extends DecoratorTokenVisitor {
    private final Set<String> _contextualKeywords;
    private final boolean _suppressWhenModifier;

    private boolean _precededByDot;
    private String _pendingName;
    private boolean _pendingWasDotPreceded;

    /**
     * Constructor.
     *
     * @param decorated            The token visitor being decorated
     * @param contextualKeywords   Words that are contextual keywords in one specific position
     *                             but ordinary identifiers everywhere else
     * @param suppressWhenModifier If {@code true}, a word confirmed to be in its genuine
     *                             keyword role is suppressed entirely (replicates
     *                             {@code IGNORE_MODIFIERS} stripping, now handled here instead
     *                             of via a generic name-based {@code MODIFIERS} set)
     */
    RecogniseAccessorKeywordTokenVisitor(final TokenVisitor decorated, final Set<String> contextualKeywords, final boolean suppressWhenModifier) {
        super(decorated);

        _contextualKeywords = Objects.requireNonNull(contextualKeywords, "contextualKeywords");
        _suppressWhenModifier = suppressWhenModifier;
    }

    @Override
    public void visit(final int lineNumber) {
        _pendingName = null;
        _precededByDot = false;
        super.visit(lineNumber);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        Objects.requireNonNull(name, "name");

        if (_pendingName != null) {
            // The pending word was followed by another identifier.
            if (_pendingWasDotPreceded) {
                // e.g. 'cache.get' with no call following - an ordinary property reference.
                flushPendingOrdinary(VARIABLE);
            } else {
                // e.g. 'get fahrenheit' - genuine keyword role confirmed.
                flushPendingKeywordRole();
            }
        }

        final boolean dot = _precededByDot;
        _precededByDot = false;

        if (_contextualKeywords.contains(name)) {
            _pendingName = name;
            _pendingWasDotPreceded = dot;
            return;
        }

        super.visitIdentifier(name, type);
    }

    @Override
    public void visitPunctuation(final char c) {
        if (_pendingName != null) {
            if (c == '(') {
                // Immediately followed by '(' - ordinary identifier being called/declared,
                // regardless of whether it was dot-preceded.
                flushPendingOrdinary(METHOD);
            } else if (_pendingWasDotPreceded) {
                flushPendingOrdinary(VARIABLE);
            } else {
                flushPendingKeywordRole();
            }
        }
        _precededByDot = c == '.';
        super.visitPunctuation(c);
    }

    @Override
    public void visitNumber(final double value) {
        flushPendingDefault();
        super.visitNumber(value);
    }

    @Override
    public void visitString(final String text, final char type) {
        flushPendingDefault();
        super.visitString(text, type);
    }

    @Override
    public void visitOther(final String s) {
        flushPendingDefault();
        super.visitOther(s);
    }

    @Override
    public void visitEnd() {
        flushPendingDefault();
        super.visitEnd();
    }

    private void flushPendingDefault() {
        _precededByDot = false;
        if (_pendingName == null) {
            return;
        }
        if (_pendingWasDotPreceded) {
            flushPendingOrdinary(VARIABLE);
        } else {
            flushPendingKeywordRole();
        }
    }

    private void flushPendingOrdinary(final int type) {
        final String name = _pendingName;
        _pendingName = null;
        super.visitIdentifier(name, type);
    }

    private void flushPendingKeywordRole() {
        final String name = _pendingName;
        _pendingName = null;
        if (!_suppressWhenModifier) {
            super.visitIdentifier(name, KEYWORD);
        }
        // else: genuine keyword role, and IGNORE_MODIFIERS is active - suppress entirely.
    }
}
