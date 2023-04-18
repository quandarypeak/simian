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
 
package com.quandarypeak.simian.gnu.trove;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * Abstract iterator class for THash implementations.  This class provides some
 * of the common iterator operations (hasNext(), remove()) and allows subclasses
 * to define the mechanism(s) for advancing the iterator and returning data.
 */
public abstract class TIterator {
    /**
     * the data structure this iterator traverses
     */
    protected final THash _hash;
    /**
     * the number of elements this iterator believes are in the
     * data structure it accesses.
     */
    protected int _expectedSize;
    /**
     * the index used for iteration.
     */
    protected int _index;

    /**
     * Create an instance of TIterator over the specified THash.
     */
    public TIterator(final THash hash) {
        _hash = hash;
        _expectedSize = _hash.size();
        _index = _hash.capacity();
    }

    /**
     * Returns true if the iterator can be advanced past its current
     * location.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean hasNext() {
        return nextIndex() >= 0;
    }

    /**
     * Removes the last entry returned by the iterator.
     * Invoking this method more than once for a single entry
     * will leave the underlying data structure in a confused
     * state.
     */
    public final void remove() {
        if (_expectedSize != _hash.size()) {
            throw new ConcurrentModificationException();
        }
        _hash.removeAt(_index);
        _expectedSize--;
    }

    /**
     * Sets the internal <code>index</code> so that the `next' object
     * can be returned.
     */
    protected final void moveToNextIndex() {
        // doing the assignment && < 0 in one line shaves
        // 3 opcodes...
        if ((_index = nextIndex()) < 0) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the index of the next value in the data structure
     * or a negative value if the iterator is exhausted.
     *
     * @return an <code>int</code> value
     */
    protected abstract int nextIndex();
} // TIterator
