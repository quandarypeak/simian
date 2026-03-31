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

/**
 * The base class for hashtables of primitive values.  Since there is
 * no notion of object equality for primitives, it isn't possible to
 * use a `REMOVED' object to track deletions in an open-addressed table.
 * So, we have to resort to using a parallel `bookkeeping' array of bytes,
 * in which flags can be set to indicate that a particular slot in the
 * hash table is FREE, FULL, or REMOVED.
 */
public abstract class TPrimitiveHash extends THash {
    /**
     * flags indicating whether each position in the hash is
     * FREE, FULL, or REMOVED
     */
    protected transient byte[] _states;

    /* constants used for state flags */

    /**
     * flag indicating that a slot in the hashtable is available
     */
    protected static final byte FREE = 0;

    /**
     * flag indicating that a slot in the hashtable is occupied
     */
    protected static final byte FULL = 1;

    /**
     * flag indicating that the value of a slot in the hashtable
     * was deleted
     */
    protected static final byte REMOVED = 2;

    /**
     * Creates a new <code>THash</code> instance with the default
     * capacity and load factor.
     */
    public TPrimitiveHash() {
    }

    /**
     * Returns the capacity of the hash table.  This is the true
     * physical capacity, without adjusting for the load factor.
     *
     * @return the physical capacity of the hash table.
     */
    @Override
    protected int capacity() {
        return _states.length;
    }

    /**
     * Delete the record at <code>index</code>.
     *
     * @param index an <code>int</code> value
     */
    @Override
    protected void removeAt(final int index) {
        super.removeAt(index);
        _states[index] = REMOVED;
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
        _states = new byte[capacity];
        return capacity;
    }
} // TPrimitiveHash
