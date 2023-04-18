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
 * A summary of check statistics.
 */
public interface CheckSummary {
    /**
     * Obtains the number of files containing duplicates.
     *
     * @return The number of files containing duplicates.
     */
    int getDuplicateFileCount();

    /**
     * Obtains the number of duplicate lines.
     *
     * @return the number of duplicate lines.
     */
    int getDuplicateLineCount();

    /**
     * Obtains the number of duplicate blocks.
     *
     * @return The number of duplicate blocks.
     */
    int getDuplicateBlockCount();

    /**
     * Obtains the number of duplicate lines as a percentage of duplicate line count.
     *
     * @return the number of duplicate lines as a percentage of duplicate line count.
     */
    int getDuplicateLinePercentage();

    /**
     * Obtains the total number of files processed.
     *
     * @return The total number of files processed.
     */
    int getTotalFileCount();

    /**
     * Obtains the total number of raw source lines processed.
     *
     * @return The total number of raw source lines processed.
     */
    int getTotalRawLineCount();

    /**
     * Obtains the total number of significant source lines processed.
     *
     * @return The total number of significant source lines processed.
     */
    int getTotalSignificantLineCount();

    /**
     * Obtains the processing time (in milliseconds).
     *
     * @return The processing time (in milliseconds).
     */
    long getProcessingTime();
}
