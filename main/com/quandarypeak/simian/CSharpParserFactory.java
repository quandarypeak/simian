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

final class CSharpParserFactory extends AbstractCFamilyParserFactory {
    private static final String BEGIN_REGION = "#region";
    private static final String END_REGION = "#endregion";
    private static final BlockMarkers REGION_MARKERS = new BlockMarkers(BEGIN_REGION, END_REGION);

    /**
     * These modifiers may be optionally ignored
     */
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList("abstract", "checked", "class", "const", "delegate", "enum", "explicit", "extern", "implicit", "interface", "internal", "override", "params", "private", "protected", "public", "readonly", "static", "ref", "sealed", "struct", "unchecked", "unsafe", "volatile"));

    /**
     * Any of these will cause the line on which it appears to be ignored
     */
    private static final Set<String> IGNORE_LINES_TRIGGERS_BASE = new HashSet<>(Arrays.asList("namespace", "using"));

    /**
     * Any of these will cause the line on which it appears to be ignored
     */
    private static final Set<String> IGNORE_LINES_TRIGGERS_ALL = new HashSet<>();

    static {
        IGNORE_LINES_TRIGGERS_ALL.addAll(IGNORE_LINES_TRIGGERS_BASE);
        IGNORE_LINES_TRIGGERS_ALL.add(BEGIN_REGION);
        IGNORE_LINES_TRIGGERS_ALL.add(END_REGION);
    }

    @Override
    public Parser createParser(final LineListener listener, final Options options) {
        Objects.requireNonNull(options, "options");

        LineListener l = listener;
        Set<String> t = IGNORE_LINES_TRIGGERS_ALL;

        if (options.hasOption(Option.IGNORE_BLOCKS)) {
            for (final BlockMarkers blockMarkers : options.<Iterable<BlockMarkers>>getOption(Option.IGNORE_BLOCKS)) {
                l = new IgnoreBlocksLineListener(l, blockMarkers);
            }
            t = IGNORE_LINES_TRIGGERS_BASE;
        }

        if (options.hasOption(Option.IGNORE_REGIONS)) {
            l = new IgnoreBlocksLineListener(l, REGION_MARKERS);
            t = IGNORE_LINES_TRIGGERS_BASE;
        }

        return new CFamilyParser(createBaseLanguageTokenVisitor(l, options, MODIFIERS, t));
    }
}
