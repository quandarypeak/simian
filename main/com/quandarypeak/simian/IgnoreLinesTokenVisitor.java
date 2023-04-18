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
 * Ignores all lines containing a trigger word.
 */
final class IgnoreLinesTokenVisitor extends DecoratorTokenVisitor {
    /**
     * The words that trigger a line to be ignored
     */
    private final Set<String> _triggerWords;

    /**
     * Should we ignore the current line?
     */
    private boolean _ignoreLine;

    /**
     * Constructor.
     *
     * @param decorated    The token visitor being decorated
     * @param triggerWords The words that trigger a line to be ignored
     */
    IgnoreLinesTokenVisitor(final TokenVisitor decorated, final Set<String> triggerWords) {
        super(decorated);
        _triggerWords = Objects.requireNonNull(triggerWords, "triggerWords");
    }

    @Override
    public void visit(final int lineNumber) {
        _ignoreLine = false;
        super.visit(lineNumber);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        if (_triggerWords.contains(name)) {
            _ignoreLine = true;
        } else {
            super.visitIdentifier(name, type);
        }
    }

    @Override
    public void visitEnd() {
        if (!_ignoreLine) {
            super.visitEnd();
        }
    }
}
