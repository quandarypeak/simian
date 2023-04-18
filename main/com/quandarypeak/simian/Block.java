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
 * A block of code.
 */
public interface Block {
    /**
     * Obtains the file containing the lines.
     *
     * @return The file containing the lines.
     */
    SourceFile getSourceFile();

    /**
     * Obtains the starting line number of the block.
     *
     * @return The starting line number of the block.
     */
    int getStartLineNumber();

    /**
     * Obtains the ending line number of the block.
     *
     * @return The ending line number of the block.
     */
    int getEndLineNumber();

    /**
     * Obtains whether this block was subsumed by a longer block or not.
     *
     * @return true if this block was subsumed by a longer block; otherwise false.
     */
    boolean isSubsumed();

    /**
     * Determines if this block overlaps another
     *
     * @param other Another block
     * @return true if this block overlaps; otherwise false.
     */
    boolean isOverlapping(Block other);
}
