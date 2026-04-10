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
