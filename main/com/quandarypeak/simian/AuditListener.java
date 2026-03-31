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

import java.io.File;
/**
 * Classes wishing to be notified of checking events should implement this interface.
 */
public interface AuditListener {
    /**
     * Called when checking begins.
     *
     * @param options The options used when performing the check
     */
    void startCheck(Options options);

    /**
     * Called when a file has been loaded.
     *
     * @param sourceFile The file that was just loaded
     */
    void fileProcessed(SourceFile sourceFile);

    /**
     * Called to indicate the start of a set of blocks of duplicate lines.
     *
     * @param lineCount The number of lines the blocks have in common
     * @param fingerprint The unique fingerprint for the blocks in this set
     */
    void startSet(int lineCount, final String fingerprint);

    /**
     * Called once for each block of duplicate lines within a set.
     *
     * @param block the duplicated block.
     */
    void block(Block block);

    /**
     * Called to indicate the end of a set of blocks of duplicate lines.
     *
     * @param text The text
     */
    void endSet(String text);

    /**
     * Called when checking has finished.
     *
     * @param summary A summary of check statistics
     */
    void endCheck(CheckSummary summary);

    /**
     * Called when an error occurs processing a file.
     *
     * @param file The file being processed
     * @param e The error that was raised
     */
    void error(File file, Throwable e);
}
