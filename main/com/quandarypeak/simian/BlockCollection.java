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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Simple collection of blocks.
 */
final class BlockCollection {
    private Entry _last;

    BlockCollection() {
    }

    public void add(final BaseBlock block) {
        _last = new Entry(_last, block);
    }

    public boolean isAuditable() {
        for (final Iterator<BaseBlock> i = iterator(); i.hasNext(); ) {
            if (!i.next().isSubsumed()) {
                return true;
            }
        }
        return false;
    }

    public boolean containsDuplicates() {
        return _last != null && _last.hasPrevious();
    }

    public void unlink() {
        for (final Iterator<BaseBlock> i = iterator(); i.hasNext(); ) {
            i.next().unlink();
        }
        _last = null;
    }

    public Iterator<BaseBlock> iterator() {
        return new BlockIterator(_last);
    }

    private static final class Entry {
        private final Entry _previous;
        private final BaseBlock _block;

        public Entry(final Entry previous, final BaseBlock block) {
            _previous = previous;
            _block = Objects.requireNonNull(block, "block");
        }

        public boolean hasPrevious() {
            return _previous != null;
        }

        public Entry getPrevious() {
            return _previous;
        }

        public BaseBlock getBlock() {
            return _block;
        }
    }

    private static final class BlockIterator implements Iterator<BaseBlock> {
        private Entry _next;

        private BlockIterator(final Entry next) {
            _next = next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return _next != null;
        }

        @Override
        public BaseBlock next() {
            if (_next != null) {
                final BaseBlock block = _next.getBlock();
                _next = _next.getPrevious();
                return block;
            }
            throw new NoSuchElementException();
        }
    }
}
