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

/**
 * An open addressed hashing implementation for long primitives.
 */
public abstract class TLongHash extends TPrimitiveHash implements TLongHashingStrategy {
    /**
     * the set of longs
     */
    protected transient long[] _set;

    /**
     * strategy used to hash values in this collection
     */
    protected final TLongHashingStrategy _hashingStrategy;

    /**
     * Creates a new <code>TLongHash</code> instance with the default
     * capacity and load factor.
     */
    public TLongHash() {
        _hashingStrategy = this;
    }

    /**
     * initializes the hashtable to a prime capacity which is at least
     * <code>initialCapacity + 1</code>.
     *
     * @param initialCapacity an <code>int</code> value
     * @return the actual capacity chosen
     */
    @Override
    protected int setUp(final int initialCapacity) {
        final int capacity;

        capacity = super.setUp(initialCapacity);
        _set = new long[capacity];
        return capacity;
    }

    /**
     * Searches the set for <code>val</code>
     *
     * @param val an <code>long</code> value
     * @return a <code>boolean</code> value
     */
    public final boolean contains(final long val) {
        return index(val) >= 0;
    }

    /**
     * Releases the element currently stored at <code>index</code>.
     *
     * @param index an <code>int</code> value
     */
    @Override
    protected void removeAt(final int index) {
        super.removeAt(index);
        _set[index] = (long) 0;
    }

    /**
     * Locates the index of <code>val</code>.
     *
     * @param val an <code>long</code> value
     * @return the index of <code>val</code> or -1 if it isn't in the set.
     */
    protected final int index(final long val) {
        final int hash;
        int probe;
        int index;
        final int length;
        final long[] set;
        final byte[] states;

        states = _states;
        set = _set;
        length = states.length;
        hash = _hashingStrategy.computeHashCode(val) & 0x7fffffff;
        index = hash % length;

        if (states[index] != FREE && (states[index] == REMOVED || set[index] != val)) {
            // see Knuth, p. 529
            probe = 1 + hash % (length - 2);

            do {
                index -= probe;
                if (index < 0) {
                    index += length;
                }
            } while (states[index] != FREE && (states[index] == REMOVED || set[index] != val));
        }

        return states[index] == FREE ? -1 : index;
    }

    /**
     * Locates the index at which <code>val</code> can be inserted.  if
     * there is already a value equal()ing <code>val</code> in the set,
     * returns that value as a negative integer.
     *
     * @param val an <code>long</code> value
     * @return an <code>int</code> value
     */
    protected final int insertionIndex(final long val) {
        final int hash;
        int probe;
        int index;
        final int length;
        final long[] set;
        final byte[] states;

        states = _states;
        set = _set;
        length = states.length;
        hash = _hashingStrategy.computeHashCode(val) & 0x7fffffff;
        index = hash % length;

        if (states[index] == FREE) {
            return index;       // empty, all done
        } else if (states[index] == FULL && set[index] == val) {
            return -index - 1;   // already stored
        } else {                // already FULL or REMOVED, must probe
            // compute the double hash
            probe = 1 + hash % (length - 2);
            // starting at the natural offset, probe until we find an
            // offset that isn't full.
            do {
                index -= probe;
                if (index < 0) {
                    index += length;
                }
            } while (states[index] == FULL && set[index] != val);

            // if the index we found was removed: continue probing until we
            // locate a free location or an element which equal()s the
            // one we have.
            if (states[index] == REMOVED) {
                final int firstRemoved = index;
                while (states[index] != FREE && (states[index] == REMOVED || set[index] != val)) {
                    index -= probe;
                    if (index < 0) {
                        index += length;
                    }
                }
                return states[index] == FULL ? -index - 1 : firstRemoved;
            }
            // if it's full, the key is already stored
            return states[index] == FULL ? -index - 1 : index;
        }
    }

    /**
     * Default implementation of TLongHashingStrategy:
     * delegates hashing to HashFunctions.hash(long).
     *
     * @param val value to hash
     * @return the hashcode.
     */
    @Override
    public final int computeHashCode(final long val) {
        return HashFunctions.hash(val);
    }
} // TLongHash
