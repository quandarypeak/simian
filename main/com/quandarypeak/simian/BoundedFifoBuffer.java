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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The BoundedFifoBuffer is a <strong>very</strong> efficient implementation of
 * Buffer that does not alter the size of the buffer at runtime.
 *
 * The removal order of a {@code BoundedFifoBuffer} is based on the
 * insertion order; elements are removed in the same order in which they
 * were added.  The iteration order is the same as the removal order.
 *
 * The {@link #add(Object)} and {@link #remove()} operations
 * all perform in constant time.  All other operations perform in linear
 * time or worse.
 *
 * Note that this implementation is not synchronized.
 *
 * This buffer prevents null objects from being added.
 */
final class BoundedFifoBuffer<T> implements Iterable<T> {
    private final Object[] elements;
    private int start;
    private int end;
    private boolean full;

    /**
     * Constructs a new {@code BoundedFifoBuffer} big enough to hold
     * the specified number of elements.
     *
     * @param capacity the maximum number of elements for this fifo
     */
    BoundedFifoBuffer(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalStateException("capacity can't be less than 1");
        }

        this.elements = new Object[capacity];
    }

    public int capacity() {
        return elements.length;
    }

    /**
     * Returns the number of elements stored in the buffer.
     *
     * @return this buffer's size
     */
    public int size() {
        final int size;

        if (end < start) {
            size = elements.length - start + end;
        } else if (end == start) {
            size = full ? elements.length : 0;
        } else {
            size = end - start;
        }

        return size;
    }

    /**
     * Returns true if this buffer is empty; false otherwise.
     *
     * @return true if this buffer is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Clears this buffer.
     */
    public void clear() {
        this.full = false;
        this.start = 0;
        this.end = 0;
        Arrays.fill(elements, null);
    }

    /**
     * Adds the given element to this buffer.
     *
     * @param element the element to add
     * @return true, always
     * @throws NullPointerException  if the given element is null
     * @throws IllegalStateException if this buffer is full
     */
    public boolean add(final T element) {
        if (element == null) {
            throw new IllegalStateException("element can't be null");
        }
        if (full) {
            throw new IllegalStateException("The buffer cannot hold more than " + elements.length + " objects.");
        }

        elements[end++] = element;

        if (end >= elements.length) {
            this.end = 0;
        }

        if (end == start) {
            this.full = true;
        }

        return true;
    }

    /**
     * Removes the least recently inserted element from this buffer.
     *
     * @return the least recently inserted element
     * @throws IllegalStateException if the buffer is empty
     */
    public T remove() {
        if (isEmpty()) {
            throw new IllegalStateException("The buffer is already empty");
        }

        @SuppressWarnings("unchecked")
        final T element = (T) elements[start];

        if (element != null) {
            elements[start++] = null;

            if (start >= elements.length) {
                this.start = 0;
            }

            this.full = false;
        }

        return element;
    }

    /**
     * Increments the internal _index.
     *
     * @param index the _index to increment
     * @return the updated _index
     */
    private int increment(int index) {
        index++;
        if (index >= elements.length) {
            index = 0;
        }
        return index;
    }

    /**
     * Returns an iterator over this buffer's elements.
     *
     * @return an iterator over this buffer's elements
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index = start;
            private int lastReturnedIndex = -1;
            private boolean first = full;

            @Override
            public boolean hasNext() {
                return first || index != end;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                this.first = false;
                this.lastReturnedIndex = index;
                this.index = increment(index);

                return (T) elements[lastReturnedIndex];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
