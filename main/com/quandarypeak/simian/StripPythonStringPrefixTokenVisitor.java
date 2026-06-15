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

/**
 * Strips Python string-prefix identifiers (f, b, r, u and their two-character combinations)
 * that StreamTokenizer emits immediately before the string token.
 *
 * <p>Python allows string literals to be decorated with a prefix: {@code f"..."}, {@code b"..."},
 * {@code r"..."}, {@code u"..."}, {@code rb"..."}, etc.  The tokenizer sees the prefix as a
 * word token followed by the string token with no intervening whitespace.  Without stripping,
 * {@code f"hello"} and {@code "hello"} produce different fingerprints — a false structural
 * difference that Simian should ignore.
 *
 * <p>This visitor buffers each candidate prefix identifier.  If the very next token is a string,
 * the prefix is silently dropped and only the string is forwarded.  If any other token arrives
 * first, the buffered identifier is flushed normally — preserving correct behaviour for the
 * rare (but legal) case where a single-letter variable happens to share a name with a prefix.
 */
final class StripPythonStringPrefixTokenVisitor extends DecoratorTokenVisitor {

    private String _pendingName;
    private int _pendingType;

    StripPythonStringPrefixTokenVisitor(final TokenVisitor decorated) {
        super(decorated);
    }

    @Override
    public void visitFile() {
        _pendingName = null;
        super.visitFile();
    }

    @Override
    public void visit(final int lineNumber) {
        _pendingName = null;
        super.visit(lineNumber);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        flushPending();
        if (isStringPrefix(name)) {
            _pendingName = name;
            _pendingType = type;
        } else {
            super.visitIdentifier(name, type);
        }
    }

    @Override
    public void visitString(final String text, final char type) {
        _pendingName = null; // prefix modified this string — discard it
        super.visitString(text, type);
    }

    @Override
    public void visitNumber(final double value) {
        flushPending();
        super.visitNumber(value);
    }

    @Override
    public void visitPunctuation(final char c) {
        flushPending();
        super.visitPunctuation(c);
    }

    @Override
    public void visitComment(final String text) {
        flushPending();
        super.visitComment(text);
    }

    @Override
    public void visitOther(final String s) {
        flushPending();
        super.visitOther(s);
    }

    @Override
    public void visitEnd() {
        flushPending();
        super.visitEnd();
    }

    private void flushPending() {
        if (_pendingName != null) {
            super.visitIdentifier(_pendingName, _pendingType);
            _pendingName = null;
        }
    }

    private static boolean isStringPrefix(final String name) {
        final int len = name.length();
        if (len == 1) {
            final char c = name.charAt(0);
            return c == 'f' || c == 'F' || c == 'b' || c == 'B'
                || c == 'r' || c == 'R' || c == 'u' || c == 'U';
        }
        if (len == 2) {
            final char c0 = Character.toLowerCase(name.charAt(0));
            final char c1 = Character.toLowerCase(name.charAt(1));
            return (c0 == 'r' && c1 == 'b') || (c0 == 'b' && c1 == 'r')
                || (c0 == 'f' && c1 == 'r') || (c0 == 'r' && c1 == 'f');
        }
        return false;
    }
}
