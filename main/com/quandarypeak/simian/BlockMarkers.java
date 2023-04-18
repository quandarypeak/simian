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
 * Strings that mark the start and end of a code block.
 */
public final class BlockMarkers {
    /**
     * The token that indicates the start of the block.
     */
    private final String _start;

    /**
     * The token that indicates the end of the block.
     */
    private final String _end;

    /**
     * Constructor.
     *
     * @param start The string that indicates the start of the block.
     * @param end   The string that indicates the end of the block.
     */
    public BlockMarkers(final String start, final String end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");

        _start = start;
        _end = end;
    }

    /**
     * Determines if the specified string signifies the start of the block.
     *
     * @param text The text.
     * @return {@code true} if the specified text signifies the start of the block; otherwise {@code false}.
     */
    public boolean isStart(final CharSequence text) {
        Objects.requireNonNull(text, "text");
        return startsWithIgnoreCase(text, _start);
    }

    /**
     * Determines if the specified string signifies the end of the block.
     *
     * @param text The text.
     * @return {@code true} if the specified text signifies the end of the block; otherwise {@code false}.
     */
    public boolean isEnd(final CharSequence text) {
        Objects.requireNonNull(text, "text");
        return startsWithIgnoreCase(text, _end);
    }

    public String toString() {
        return _start + ":" + _end;
    }

    static BlockMarkers valueOf(final String s) {
        Objects.requireNonNull(s);

        final int colon = s.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("invalid block markers");
        }

        final String start = s.substring(0, colon).trim();
        final String end = s.substring(colon + 1).trim();

        if (start.isEmpty()) {
            throw new IllegalArgumentException("invalid block markers");
        }
        if (end.isEmpty()) {
            throw new IllegalArgumentException("invalid block markers");
        }

        return new BlockMarkers(start, end);
    }

    private static boolean startsWithIgnoreCase(final CharSequence text, final String s) {
        return text.length() >= s.length() && text.subSequence(0, s.length()).toString().equalsIgnoreCase(s);
    }
}
