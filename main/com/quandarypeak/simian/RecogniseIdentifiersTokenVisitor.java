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
import java.util.Set;

/**
 * Intercepts all visits to a words and applies some heuristics to try and distinguish between type names and other
 * words such as identifiers, modifiers, etc.
 */
final class RecogniseIdentifiersTokenVisitor extends DecoratorTokenVisitor {
    private final Set<String> _types;
    private final Set<String> _keywords;

    private String _variableOrMethodName;

    /**
     * Constructor.
     *
     * @param decorated The token visitor being decorated
     * @param types     Words that are considered to be types
     * @param keywords  Words that are always left untouched
     */
    RecogniseIdentifiersTokenVisitor(final TokenVisitor decorated, final Set<String> types, final Set<String> keywords) {
        super(decorated);

        Objects.requireNonNull(types, "types");
        Objects.requireNonNull(keywords, "keywords");

        _types = types;
        _keywords = keywords;
    }

    @Override
    public void visit(final int lineNumber) {
        _variableOrMethodName = null;
        super.visit(lineNumber);
    }

    @Override
    public void visitNumber(final double value) {
        xxx();
        super.visitNumber(value);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        Objects.requireNonNull(name, "name");

        xxx();

        if (_types.contains(name)) {
            super.visitIdentifier(name, TYPE);
        } else if (_keywords.contains(name)) {
            super.visitIdentifier(name, KEYWORD);
        } else if (isAllUpperCase(name)) {
            super.visitIdentifier(name, CONSTANT);
        } else if (isTypeName(name)) {
            super.visitIdentifier(name, TYPE);
        } else {
            _variableOrMethodName = name;
        }
    }

    @Override
    public void visitString(final String text, final char type) {
        xxx();
        super.visitString(text, type);
    }

    @Override
    public void visitPunctuation(final char c) {
        if (_variableOrMethodName != null) {
            if (c == '(') {
                super.visitIdentifier(_variableOrMethodName, METHOD);
            } else {
                super.visitIdentifier(_variableOrMethodName, VARIABLE);
            }
            _variableOrMethodName = null;
        }
        super.visitPunctuation(c);
    }

    @Override
    public void visitEnd() {
        xxx();
        super.visitEnd();
    }

    private void xxx() {
        if (_variableOrMethodName != null) {
            super.visitIdentifier(_variableOrMethodName, VARIABLE);
            _variableOrMethodName = null;
        }
    }

    private boolean isAllUpperCase(final String name) {
        for (int i = name.length() - 1; i >= 0; --i) {
            if (Character.isLowerCase(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isTypeName(final String name) {
        return Character.isUpperCase(name.charAt(0)) && name.length() > 1;
    }
}
