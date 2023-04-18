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
 * Converts all characters to lower case effectively ignoring the case completely.
 */
final class IgnoreCharacterCaseTokenVisitor extends DecoratorTokenVisitor {
    /**
     * Constructor.
     *
     * @param decorated The token visitor being decorated
     */
    IgnoreCharacterCaseTokenVisitor(final TokenVisitor decorated) {
        super(decorated);
    }

    @Override
    public void visitString(final String text, final char type) {
        Objects.requireNonNull(text, "text");

        if (type == '\'') {
            super.visitString(text.toLowerCase(), '\'');
        } else {
            super.visitString(text, type);
        }
    }
}
