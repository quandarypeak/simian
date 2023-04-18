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

final class RecogniseRubyBlockCommentKeywordsTokenVisitor extends DecoratorTokenVisitor {
    RecogniseRubyBlockCommentKeywordsTokenVisitor(final TokenVisitor decorated) {
        super(decorated);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        if (name.contains("=")) {
            visitEqualsPrefixedIdentifier(name);
        } else {
            super.visitIdentifier(name, type);
        }
    }

    private void visitEqualsPrefixedIdentifier(final String name) {
        if ("=begin".equals(name) || "=end".equals(name)) {
            super.visitIdentifier(name, TokenVisitor.KEYWORD);
        } else {
            visitEqualsSeparatedIdentifier(name);
        }
    }

    private void visitEqualsSeparatedIdentifier(final String name) {
        int lastIndex = 0;
        int index = name.indexOf('=');
        while (index != -1) {
            visitIdentifierIfPresent(name, lastIndex, index);
            visitPunctuation('=');
            lastIndex = index;
            index = name.indexOf('=', index + 1);
        }
        visitIdentifierIfPresent(name, lastIndex + 1, name.length());
    }

    private void visitIdentifierIfPresent(final String name, final int startIndex, final int endIndex) {
        if (startIndex < endIndex) {
            super.visitIdentifier(name.substring(startIndex, endIndex), TokenVisitor.UNKNOWN);
        }
    }
}
