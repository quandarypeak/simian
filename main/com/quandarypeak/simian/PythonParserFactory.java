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

final class PythonParserFactory extends AbstractCFamilyParserFactory {
//    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList("begin", "end", "class", "def", "module", "private", "protected", "public", "do"));

    @Override
    public final Parser createParser(final LineListener listener, final Options options) {
        final TokenVisitor visitor = createBaseLanguageTokenVisitor(listener, options);
        return new PythonParser(visitor);
    }
}
