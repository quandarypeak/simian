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

import java.util.NoSuchElementException;

/**
 * Iterator for maps of type long and Object.
 *
 * The iterator semantics for Trove's primitive maps is slightly different
 * from those defined in <code>java.util.Iterator</code>, but still well within
 * the scope of the pattern, as defined by Gamma, et al.
 *
 * This iterator does <b>not</b> implicitly advance to the next entry when
 * the value at the current position is retrieved.  Rather, you must explicitly
 * ask the iterator to <code>advance()</code> and then retrieve either the <code>key()</code>,
 * the <code>value()</code> or both.  This is done so that you have the option, but not
 * the obligation, to retrieve keys and/or values as your application requires, and
 * without introducing wrapper objects that would carry both.  As the iteration is
 * stateful, access to the key/value parts of the current map entry happens in
 * constant time.
 *
 * In practice, the iterator is akin to a "search finger" that you move from
 * position to position.  Read or write operations affect the current entry only and
 * do not assume responsibility for moving the finger.
 *
 * Here are some sample scenarios for this class of iterator:
 *
 * <pre>
 * // accessing keys/values through an iterator:
 * for (TLongObjectIterator it = map.iterator();
 *      it.hasNext();) {
 *   it.forward();
 *   if (satisfiesCondition(it.key()) {
 *     doSomethingWithValue(it.value());
 *   }
 * }
 * </pre>
 *
 * <pre>
 * // modifying values in-place through iteration:
 * for (TLongObjectIterator it = map.iterator();
 *      it.hasNext();) {
 *   it.forward();
 *   if (satisfiesCondition(it.key()) {
 *     it.setValue(newValueForKey(it.key()));
 *   }
 * }
 * </pre>
 *
 * <pre>
 * // deleting entries during iteration:
 * for (TLongObjectIterator it = map.iterator();
 *      it.hasNext();) {
 *   it.forward();
 *   if (satisfiesCondition(it.key()) {
 *     it.remove();
 *   }
 * }
 * </pre>
 *
 * <pre>
 * // faster iteration by avoiding hasNext():
 * TLongObjectIterator iterator = map.iterator();
 * for (int i = map.size(); i-- > 0;) {
 *   iterator.advance();
 *   doSomethingWithKeyAndValue(iterator.key(), iterator.value());
 * }
 * </pre>
 */
public final class TLongObjectIterator extends TPrimitiveIterator {
    /**
     * the collection being iterated over
     */
    private final TLongObjectHashMap _map;

    /**
     * Creates an iterator over the specified map
     */
    public TLongObjectIterator(final TLongObjectHashMap map) {
        super(map);
        _map = map;
    }

    /**
     * Moves the iterator forward to the next entry in the underlying map.
     *
     * @throws NoSuchElementException if the iterator is already exhausted
     */
    public final void advance() {
        moveToNextIndex();
    }

    /**
     * Provides access to the key of the mapping at the iterator's position.
     * Note that you must <code>advance()</code> the iterator at least once
     * before invoking this method.
     *
     * @return the key of the entry at the iterator's current position.
     */
    public long key() {
        return _map._set[_index];
    }

    /**
     * Provides access to the value of the mapping at the iterator's position.
     * Note that you must <code>advance()</code> the iterator at least once
     * before invoking this method.
     *
     * @return the value of the entry at the iterator's current position.
     */
    public final Object value() {
        return _map._values[_index];
    }

    /**
     * Replace the value of the mapping at the iterator's position with the
     * specified value. Note that you must <code>advance()</code> the iterator at
     * least once before invoking this method.
     *
     * @param val the value to set in the current entry
     * @return the old value of the entry.
     */
    public Object setValue(final Object val) {
        final Object old = value();
        _map._values[_index] = val;
        return old;
    }
}// TLongObjectIterator
