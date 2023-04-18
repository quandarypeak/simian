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
