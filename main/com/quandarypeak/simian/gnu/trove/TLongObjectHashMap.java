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
 * An open addressed Map implementation for long keys and Object values.
 */
public final class TLongObjectHashMap extends TLongHash {

    /**
     * the values of the map
     */
    protected transient Object[] _values;

    /**
     * Creates a new <code>TLongObjectHashMap</code> instance with the default
     * capacity and load factor.
     */
    public TLongObjectHashMap() {
    }

    /**
     * @return a TLongObjectIterator with access to this map's keys and values
     */
    public final TLongObjectIterator iterator() {
        return new TLongObjectIterator(this);
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
        _values = new Object[capacity];
        return capacity;
    }

    /**
     * Inserts a key/value pair into the map.
     *
     * @param key   an <code>long</code> value
     * @param value an <code>Object</code> value
     * @return the previous value associated with <code>key</code>,
     * or null if none was found.
     */
    public final Object put(final long key, final Object value) {
        final byte previousState;
        Object previous = null;
        int index = insertionIndex(key);
        boolean isNewMapping = true;
        if (index < 0) {
            index = -index - 1;
            previous = _values[index];
            isNewMapping = false;
        }
        previousState = _states[index];
        _set[index] = key;
        _states[index] = FULL;
        _values[index] = value;
        if (isNewMapping) {
            postInsertHook(previousState == FREE);
        }

        return previous;
    }

    /**
     * rehashes the map to the new capacity.
     *
     * @param newCapacity an <code>int</code> value
     */
    @Override
    protected void rehash(final int newCapacity) {
        final int oldCapacity = _set.length;
        final long[] oldKeys = _set;
        final Object[] oldVals = _values;
        final byte[] oldStates = _states;

        _set = new long[newCapacity];
        _values = new Object[newCapacity];
        _states = new byte[newCapacity];

        for (int i = oldCapacity; i-- > 0; ) {
            if (oldStates[i] == FULL) {
                final long o = oldKeys[i];
                final int index = insertionIndex(o);
                _set[index] = o;
                _values[index] = oldVals[i];
                _states[index] = FULL;
            }
        }
    }

    /**
     * retrieves the value for <code>key</code>
     *
     * @param key an <code>long</code> value
     * @return the value of <code>key</code> or null if no such mapping exists.
     */
    public final Object get(final long key) {
        final int index = index(key);
        return index < 0 ? null : _values[index];
    }

    /**
     * Empties the map.
     */
    @Override
    public final void clear() {
        super.clear();
        final long[] keys = _set;
        final Object[] vals = _values;
        final byte[] states = _states;

        for (int i = keys.length; i-- > 0; ) {
            keys[i] = (long) 0;
            vals[i] = null;
            states[i] = FREE;
        }
    }

    /**
     * Deletes a key/value pair from the map.
     *
     * @param key an <code>long</code> value
     * @return an <code>Object</code> value
     */
    public Object remove(final long key) {
        Object prev = null;
        final int index = index(key);
        if (index >= 0) {
            prev = _values[index];
            removeAt(index);    // clear key,state; adjust size
        }
        return prev;
    }

    /**
     * removes the mapping at <code>index</code> from the map.
     *
     * @param index an <code>int</code> value
     */
    @Override
    protected final void removeAt(final int index) {
        super.removeAt(index);  // clear key, state; adjust size
        _values[index] = null;
    }

    /**
     * Returns the values of the map.
     *
     * @return a <code>Collection</code> value
     */
    public Object[] getValues() {
        final Object[] vals = new Object[size()];
        final Object[] v = _values;
        final byte[] states = _states;

        for (int i = v.length, j = 0; i-- > 0; ) {
            if (states[i] == FULL) {
                vals[j++] = v[i];
            }
        }
        return vals;
    }

    /**
     * returns the keys of the map.
     *
     * @return a <code>Set</code> value
     */
    public long[] keys() {
        final long[] keys = new long[size()];
        final long[] k = _set;
        final byte[] states = _states;

        for (int i = k.length, j = 0; i-- > 0; ) {
            if (states[i] == FULL) {
                keys[j++] = k[i];
            }
        }
        return keys;
    }

    /**
     * checks for the presence of <code>val</code> in the values of the map.
     *
     * @param val an <code>Object</code> value
     * @return a <code>boolean</code> value
     */
    public boolean containsValue(final Object val) {
        final byte[] states = _states;
        final Object[] vals = _values;

        // special case null values so that we don't have to
        // perform null checks before every call to equals()
        if (null == val) {
            for (int i = vals.length; i-- > 0; ) {
                if (states[i] == FULL && val == vals[i]) {
                    return true;
                }
            }
        } else {
            for (int i = vals.length; i-- > 0; ) {
                if (states[i] == FULL && (val == vals[i] || val.equals(vals[i]))) {
                    return true;
                }
            }
        } // end of else
        return false;
    }


    /**
     * checks for the present of <code>key</code> in the keys of the map.
     *
     * @param key an <code>long</code> value
     * @return a <code>boolean</code> value
     */
    public boolean containsKey(final long key) {
        return contains(key);
    }
}
