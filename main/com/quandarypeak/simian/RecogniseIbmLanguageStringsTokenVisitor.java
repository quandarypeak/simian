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

final class RecogniseIbmLanguageStringsTokenVisitor extends DecoratorTokenVisitor {
    RecogniseIbmLanguageStringsTokenVisitor(final TokenVisitor decorated) {
        super(decorated);
    }

    @Override
    public void visitOther(final String s) {
        if (s.endsWith("'")) {
            if (s.length() == 1) {
                super.visitPunctuation('\'');
            } else {
                final int from = s.startsWith("=") ? 1 : 0;
                if (isString(s, from)) {
                    super.visitString(s.substring(s.indexOf('\'') + 1, s.length() - 1), '"');
                    return;
                }
            }
        }

        super.visitOther(s);
    }

    private boolean isString(final String s, final int from) {
        return isSelfDefiningTerm(s, from) || isLiteral(s, from);
    }

    private boolean isLiteral(final String s, final int from) {
        return s.startsWith("'", from);
    }

    private boolean isSelfDefiningTerm(final String s, final int from) {
        return s.startsWith("C", from);
    }

}
