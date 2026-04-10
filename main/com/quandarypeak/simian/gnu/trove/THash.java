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

package com.quandarypeak.simian.gnu.trove;

public abstract class THash {
    /**
     * the current number of occupied slots in the hash.
     */
    protected transient int _size;

    /**
     * the current number of free slots in the hash.
     */
    protected transient int _free;

    /**
     * the load above which rehashing occurs.
     */
    protected static final float DEFAULT_LOAD_FACTOR = 0.5f;

    /**
     * the default initial capacity for the hash table.  This is one
     * less than a prime value because one is added to it when
     * searching for a prime capacity to account for the free slot
     * required by open addressing. Thus, the real default capacity is
     * 11.
     */
    protected static final int DEFAULT_INITIAL_CAPACITY = 10;

    /**
     * Determines how full the internal table can become before
     * rehashing is required. This must be a value in the range: 0.0 &lt;
     * loadFactor &lt; 1.0.  The default value is 0.5, which is about as
     * large as you can get in open addressing without hurting
     * performance.  Cf. Knuth, Volume 3., Chapter 6.
     */
    protected final float _loadFactor;

    /**
     * The maximum number of elements allowed without allocating more space.
     */
    protected int _maxSize;

    /**
     * Creates a new <code>THash</code> instance with the default
     * capacity and load factor.
     */
    public THash() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new <code>THash</code> instance with a prime capacity
     * at or near the specified capacity and with the default load
     * factor.
     *
     * @param initialCapacity an <code>int</code> value
     */
    public THash(final int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new <code>THash</code> instance with a prime capacity
     * at or near the minimum needed to hold <code>initialCapacity</code>
     * elements with load factor <code>loadFactor</code> without triggering
     * a rehash.
     *
     * @param initialCapacity an <code>int</code> value
     * @param loadFactor      a <code>float</code> value
     */
    public THash(final int initialCapacity, final float loadFactor) {
        _loadFactor = loadFactor;
        setUp((int) Math.ceil(initialCapacity / loadFactor));
    }

    /**
     * Tells whether this set is currently holding any elements.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isEmpty() {
        return 0 == _size;
    }

    /**
     * Returns the number of distinct elements in this collection.
     *
     * @return an <code>int</code> value
     */
    public final int size() {
        return _size;
    }

    /**
     * @return the current physical capacity of the hash table.
     */
    protected abstract int capacity();

    /**
     * Ensure that this hashtable has sufficient capacity to hold
     * <code>desiredCapacity</code> <b>additional</b> elements without
     * requiring a rehash.  This is a tuning method you can call
     * before doing a large insert.
     *
     * @param desiredCapacity an <code>int</code> value
     */
    public void ensureCapacity(final int desiredCapacity) {
        if (desiredCapacity > _maxSize - size()) {
            rehash(TPrimeFinder.nextPrime((int) Math.ceil(desiredCapacity + size() / _loadFactor) + 1));
            computeMaxSize(capacity());
        }
    }

    /**
     * Compresses the hashtable to the minimum prime size (as defined
     * by PrimeFinder) that will hold all of the elements currently in
     * the table.  If you have done a lot of <code>remove</code>
     * operations and plan to do a lot of queries or insertions or
     * iteration, it is a good idea to invoke this method.  Doing so
     * will accomplish two things:
     *
     * <ol>
     * <li> You'll free memory allocated to the table but no
     * longer needed because of the remove()s.</li>
     *
     * <li> You'll get better query/insert/iterator performance
     * because there won't be any <code>REMOVED</code> slots to skip
     * over when probing for indices in the table.</li>
     * </ol>
     */
    public final void compact() {
        // need at least one free spot for open addressing
        rehash(TPrimeFinder.nextPrime((int) Math.ceil(size() / _loadFactor) + 1));
        computeMaxSize(capacity());
    }

    /**
     * This simply calls {@link #compact compact}.  It is included for
     * symmetry with other collection classes.  Note that the name of this
     * method is somewhat misleading (which is why we prefer
     * <code>compact</code>) as the load factor may require capacity above
     * and beyond the size of this collection.
     *
     * @see #compact
     */
    public final void trimToSize() {
        compact();
    }

    /**
     * Delete the record at <code>index</code>.  Reduces the size of the
     * collection by one.
     *
     * @param index an <code>int</code> value
     */
    protected void removeAt(final int index) {
        _size--;
    }

    /**
     * Empties the collection.
     */
    public void clear() {
        _size = 0;
        _free = capacity();
    }

    /**
     * initializes the hashtable to a prime capacity which is at least
     * <code>initialCapacity + 1</code>.
     *
     * @param initialCapacity an <code>int</code> value
     * @return the actual capacity chosen
     */
    protected int setUp(final int initialCapacity) {
        final int capacity;

        capacity = TPrimeFinder.nextPrime(initialCapacity);
        computeMaxSize(capacity);
        return capacity;
    }

    /**
     * Rehashes the set.
     *
     * @param newCapacity an <code>int</code> value
     */
    protected abstract void rehash(int newCapacity);

    /**
     * Computes the values of maxSize. There will always be at least
     * one free slot required.
     *
     * @param capacity an <code>int</code> value
     */
    private final void computeMaxSize(final int capacity) {
        // need at least one free slot for open addressing
        _maxSize = Math.min(capacity - 1, (int) Math.floor(capacity * _loadFactor));
        _free = capacity - _size; // reset the free element count
    }

    /**
     * After an insert, this hook is called to adjust the size/free
     * values of the set and to perform rehashing if necessary.
     */
    protected final void postInsertHook(final boolean usedFreeSlot) {
        if (usedFreeSlot) {
            _free--;
        }

        // rehash whenever we exhaust the available space in the table
        if (++_size > _maxSize || _free == 0) {
            rehash(TPrimeFinder.nextPrime(capacity() << 1));
            computeMaxSize(capacity());
        }
    }
}// THash
