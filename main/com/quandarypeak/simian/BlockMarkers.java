/*
 * Copyright 2022-2026 Quandary Peak Research, Inc.
 * Original authorship by Simon Harris.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
