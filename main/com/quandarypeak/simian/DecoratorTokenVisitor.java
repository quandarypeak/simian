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

class DecoratorTokenVisitor implements TokenVisitor {
    private final TokenVisitor _decorated;

    DecoratorTokenVisitor(final TokenVisitor decorated) {
        _decorated = Objects.requireNonNull(decorated, "decorated");;
    }

    @Override
    public void visitFile() {
        _decorated.visitFile();
    }

    @Override
    public void visit(final int lineNumber) {
        _decorated.visit(lineNumber);
    }

    @Override
    public void visitNumber(final double value) {
        _decorated.visitNumber(value);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        _decorated.visitIdentifier(name, type);
    }

    @Override
    public void visitString(final String text, final char type) {
        _decorated.visitString(text, type);
    }

    @Override
    public void visitComment(final String text) {
        _decorated.visitComment(text);
    }

    @Override
    public void visitPunctuation(final char c) {
        _decorated.visitPunctuation(c);
    }

    @Override
    public void visitOther(final String s) {
        _decorated.visitOther(s);
    }

    @Override
    public void visitEnd() {
        _decorated.visitEnd();
    }
}
