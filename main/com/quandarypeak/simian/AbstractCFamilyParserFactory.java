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
 * Convenience base class for c-style languages such as C#, C++ and C itself.
 */
abstract class AbstractCFamilyParserFactory extends AsbtractParserFactory {
    protected final TokenVisitor createBaseLanguageTokenVisitor(final LineListener listener, final Options options, final Set<String> modifiers, final Set<String> ignoreLineTriggers) {
        Objects.requireNonNull(options);

        TokenVisitor visitor = createBaseLanguageTokenVisitor(listener, options);

        if (options.hasOption(Option.BALANCE_SQUARE_BRACKETS)) {
            visitor = new BalanceSquareBracketsTokenVisitor(visitor);
        }

        if (options.hasOption(Option.IGNORE_CURLY_BRACES)) {
            visitor = new IgnoreCurlyBracesTokenVisitor(visitor);
        }

        if (options.hasOption(Option.IGNORE_MODIFIERS)) {
            visitor = new IgnoreWordsTokenVisitor(modifiers, visitor);
        }

        visitor = new IgnoreLinesTokenVisitor(visitor, ignoreLineTriggers);

        return visitor;
    }
}
