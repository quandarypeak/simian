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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class RubyParserFactory extends AbstractCFamilyParserFactory {
    private static final BlockMarkers COMMENT_BLOCK_MARKERS = new BlockMarkers("=begin", "=end");
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList("begin", "end", "class", "def", "module", "private", "protected", "public", "do"));
    private static final Set<String> IGNORE_LINE_TRIGGERS = new HashSet<>(Arrays.asList("require", "require_gem"));

    @Override
    public final Parser createParser(final LineListener listener, final Options options) {
        Objects.requireNonNull(options, "options");

        TokenVisitor visitor = createBaseLanguageTokenVisitor(new IgnoreBlocksLineListener(listener, COMMENT_BLOCK_MARKERS), options, MODIFIERS, IGNORE_LINE_TRIGGERS);

        visitor = new RecogniseRubyBlockCommentKeywordsTokenVisitor(visitor);

        return new RubyParser(visitor);
    }
}
