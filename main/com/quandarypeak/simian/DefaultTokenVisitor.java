/*
 * Simian Similarity Analyzer
 * 
 * Copyright (c) 2023 Quandary Peak Research.
 * Original authorship by Simon Harris.
 * 
 * Use of this software is permitted for educational or academic research
 * purposes only and is subject to the Quandary Peak Academic Software License.
 * See docs/license.txt for details.
 * 
 * Redistribution of this software in source or binary form is not permitted.
 * 
 * For non-academic or commercial use, please contact simian@quandarypeak.com.
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
