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
 * Ignores instances of specified words (eg 'public', 'private', 'protected', 'static', 'final').
 */
final class IgnoreWordsTokenVisitor extends DecoratorTokenVisitor {
    /**
     * Words to ignore
     */
    private final Set<String> words;

    /**
     * Constructor.
     *
     * @param words     Words to ignore
     * @param decorated The token visitor being decorated
     */
    IgnoreWordsTokenVisitor(final Set<String> words, final TokenVisitor decorated) {
        super(decorated);
        this.words = Objects.requireNonNull(words);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        if (!words.contains(name)) {
            super.visitIdentifier(name, type);
        }
    }
}
