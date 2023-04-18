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

/**
 * Implements all iterator functions for the hashed object set.
 * Subclasses may override objectAtIndex to vary the object
 * returned by calls to next() (e.g. for values, and Map.Entry
 * objects).
 *
 * Note that iteration is fastest if you forego the calls to
 * <code>hasNext</code> in favor of checking the size of the structure
 * yourself and then call next() that many times:
 *
 * <pre>
 * Iterator i = collection.iterator();
 * for (int size = collection.size(); size-- > 0;) {
 *   Object o = i.next();
 * }
 * </pre>
 *
 * You may, of course, use the hasNext(), next() idiom too if
 * you aren't in a performance critical spot.
 */
public abstract class TPrimitiveIterator extends TIterator {
    /**
     * the collection on which this iterator operates.
     */
    protected final TPrimitiveHash _hash;

    /**
     * Creates a TPrimitiveIterator for the specified collection.
     */
    public TPrimitiveIterator(final TPrimitiveHash hash) {
        super(hash);
        _hash = hash;
    }

    /**
     * Returns the index of the next value in the data structure
     * or a negative value if the iterator is exhausted.
     *
     * @return an <code>int</code> value
     * @throws ConcurrentModificationException if the underlying collection's
     *                                         size has been modified since the iterator was created.
     */
    @Override
    protected final int nextIndex() {
        if (_expectedSize != _hash.size()) {
            throw new ConcurrentModificationException();
        }

        final byte[] states = _hash._states;
        int i = _index;
        while (i-- > 0 && states[i] != TPrimitiveHash.FULL) {
        }
        return i;
    }
}
