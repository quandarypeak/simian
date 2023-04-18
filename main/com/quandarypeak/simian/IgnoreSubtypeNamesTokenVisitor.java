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
 * Converts types (eg 'StringBuilder', 'StringBufferReader') to the last word starting with an upper case letter (ie
 * 'Buffer', 'Reader').
 */
final class IgnoreSubtypeNamesTokenVisitor extends DecoratorTokenVisitor {
    /**
     * Constructor.
     *
     * @param decorated The token visitor being decorated
     */
    IgnoreSubtypeNamesTokenVisitor(final TokenVisitor decorated) {
        super(decorated);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        Objects.requireNonNull(name, "name");

        if (type != TYPE) {
            super.visitIdentifier(name, type);
            return;
        }

        int index = name.length();

        while (--index > 0 && !Character.isUpperCase(name.charAt(index))) {
            // Do nothing
        }

        super.visitIdentifier(name.substring(index), type);
    }
}
