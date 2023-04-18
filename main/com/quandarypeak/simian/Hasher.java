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

/**
 * Utility class for generating hashcodes.
 */
final class Hasher {
    /**
     * The magic multiplier. Notice that it's prime? This is VERY IMPORTANT!
     */
    private static final int MAGIC_NUMBER = 31;

    private Hasher() {
        throw new UnsupportedOperationException("Constructor should not be called");
    }

    public static long hash(final long hash1, final long hash2) {
        return MAGIC_NUMBER * hash1 + hash2;
    }
}
