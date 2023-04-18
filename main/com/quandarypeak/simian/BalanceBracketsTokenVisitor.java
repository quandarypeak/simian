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

class BalanceBracketsTokenVisitor extends DecoratorTokenVisitor {
    private final char openBracket;
    private final char closeBracket;
    private int outstanding;

    BalanceBracketsTokenVisitor(final char openBracket, final char closeBracket, final TokenVisitor decorated) {
        super(decorated);
        this.openBracket = openBracket;
        this.closeBracket = closeBracket;
    }

    @Override
    public void visitFile() {
        outstanding = 0;
        super.visitFile();
    }

    @Override
    public void visit(final int lineNumber) {
        if (!hasOutstanding()) {
            super.visit(lineNumber);
        }
    }

    @Override
    public void visitPunctuation(final char c) {
        if (c == openBracket) {
            ++outstanding;
        } else if (c == closeBracket) {
            --outstanding;
            if (outstanding < 0) {
                outstanding = 0;
            }
        }

        super.visitPunctuation(c);
    }

    @Override
    public void visitEnd() {
        if (!hasOutstanding()) {
            super.visitEnd();
        }
    }

    private boolean hasOutstanding() {
        return outstanding > 0;
    }
}
