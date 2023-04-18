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
 * Interface to support pluggable hashing strategies in maps and sets.
 * Implementors can use this interface to make the trove hashing
 * algorithms use an optimal strategy when computing hashcodes.
 */
public interface TLongHashingStrategy {
    /**
     * Computes a hash code for the specified long.  Implementors
     * can use the long's own value or a custom scheme designed to
     * minimize collisions for a known set of input.
     *
     * @param val for which the hashcode is to be computed
     * @return the hashCode
     */
    int computeHashCode(long val);
}
