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

final class LineBuffer implements CharSequence {
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
