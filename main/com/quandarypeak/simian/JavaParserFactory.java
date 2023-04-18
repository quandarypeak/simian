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
import java.util.Set;

class JavaParserFactory extends AbstractCFamilyParserFactory {
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("boolean", "byte", "char", "double", "float", "int", "long", "short", "void"));

    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList("abstract", "class", "extends", "final", "implements", "interface", "native", "private", "protected", "public", "static", "synchronized", "throws", "transient", "volatile"));

    private static final Set<String> IGNORE_LINE_TRIGGERS = new HashSet<>(Arrays.asList("assert", "import", "package"));

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList("for", "do", "while", "if", "else", "goto", "break", "continue", "switch", "return", "case", "new", "try", "catch", "finally", "throw", "instanceof"));

    static {
        KEYWORDS.addAll(MODIFIERS);
        KEYWORDS.addAll(IGNORE_LINE_TRIGGERS);
    }

    @Override
    public Parser createParser(final LineListener listener, final Options options) {
        TokenVisitor visitor = createBaseLanguageTokenVisitor(listener, options, MODIFIERS, IGNORE_LINE_TRIGGERS);

        visitor = new RecogniseIdentifiersTokenVisitor(visitor, TYPES, KEYWORDS);

        return new CFamilyParser(visitor);
    }
}
