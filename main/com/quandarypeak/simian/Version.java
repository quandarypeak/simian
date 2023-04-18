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
 * Simple interface for holding the software version.
 */
public final class Version {
    /**
     * The software version.
     */
    public static final String VERSION = "${build.number}";

    /**
     * The copyright message.
     */
    public static final String BANNER = "Simian Similarity Analyzer " + VERSION + " - https://simian.quandarypeak.com" + "\n${copyright}" + "\n${license}";

    private Version() {
        throw new UnsupportedOperationException("Constructor should not be called");
    }
}
