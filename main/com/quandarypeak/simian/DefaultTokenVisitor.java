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

import java.util.Objects;

/**
 * Simple line builder that appends all tokens to build a line and notifies a listener.
 */
final class DefaultTokenVisitor implements TokenVisitor {
    private final LineListener _listener;
    private LineBuffer _line;
    private int _lineNumber;
    private boolean _needSpace;

    DefaultTokenVisitor(final LineListener listener) {
        Objects.requireNonNull(listener, "listener");
        _listener = listener;
    }

    @Override
    public void visitFile() {
        _listener.file();
    }

    @Override
    public void visit(final int lineNumber) {
        _line = new LineBuffer();
        _lineNumber = lineNumber;
        _needSpace = false;
    }

    @Override
    public void visitNumber(final double value) {
        appendSpaceIfNeeded();
        _line.append(value);
        _needSpace = false;
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        appendSpaceIfNeeded();
        _line.append(name);
        _needSpace = true;
    }

    @Override
    public void visitString(final String text, final char type) {
        _line.append(type).append(text).append(type);
        _needSpace = false;
    }

    @Override
    public void visitComment(final String text) {
        // Ignore it
    }

    @Override
    public void visitPunctuation(final char c) {
        _line.append(c);
        _needSpace = false;
    }

    @Override
    public void visitOther(final String s) {
        appendSpaceIfNeeded();
        _line.append(s);
        _needSpace = true;
    }

    @Override
    public void visitEnd() {
        if (!_line.isEmpty()) {
            _listener.line(_lineNumber, _line);
        }
    }

    private void appendSpaceIfNeeded() {
        if (_needSpace) {
            _line.append(' ');
        }
    }
}
