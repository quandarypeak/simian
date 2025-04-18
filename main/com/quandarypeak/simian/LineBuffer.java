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

public final class LineBuffer implements CharSequence {
    private static final int INITIAL_BUFFER_SIZE = 128;

    private final StringBuilder _buffer = new StringBuilder(INITIAL_BUFFER_SIZE);
    private long _lineHash;

    LineBuffer() {
    }

    LineBuffer(final String s) {
        append(s);
    }

    public LineBuffer append(final char c) {
        _buffer.append(c);
        _lineHash = Hasher.hash(_lineHash, c);
        return this;
    }

    public LineBuffer append(final double d) {
        return append(Double.toString(d));
    }

    public LineBuffer append(final String s) {
        Objects.requireNonNull(s, "s");

        _buffer.append(s);
        _lineHash = Hasher.hash(_lineHash, s.hashCode());
        return this;
    }

    public LineBuffer from(final int pos) {
        _buffer.delete(0, pos);
        return this;
    }

    public boolean contains(final String s) {
        return _buffer.indexOf(s) != -1;
    }

    public boolean startsWith(final String s) {
        Objects.requireNonNull(s, "s");
        return s.length() <= _buffer.length() && startsWith(s, 0);
    }

    public boolean startsWith(final char c) {
        return _buffer.length() > 0 && _buffer.charAt(0) == c;
    }

    public boolean isEmpty() {
        return _buffer.length() == 0;
    }

    public long getLineHash() {
        return _lineHash;
    }

    @Override
    public int length() {
        return _buffer.length();
    }

    @Override
    public char charAt(final int index) {
        return _buffer.charAt(index);
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return _buffer.subSequence(start, end);
    }

    public String toString() {
        return _buffer.toString();
    }

    private boolean startsWith(final String s, final int from) {
        for (int i = from; i < s.length(); ++i) {
            if (s.charAt(i) != _buffer.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
