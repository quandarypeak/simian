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

/**
 * Ignores the contents of strings entirely.
 */
final class IgnoreStringsTokenVisitor extends DecoratorTokenVisitor {
    private static final char REPLACEMENT = '"';

    /**
     * Constructor.
     *
     * @param decorated The token visitor being decorated
     */
    IgnoreStringsTokenVisitor(final TokenVisitor decorated) {
        super(decorated);
    }

    @Override
    public void visitString(final String text, final char type) {
        if (type == '"') {
            super.visitPunctuation(REPLACEMENT);
        } else {
            super.visitString(text, type);
        }
    }
}
