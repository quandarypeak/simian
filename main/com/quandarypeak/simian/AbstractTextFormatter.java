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

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Simply logs sets of duplicate blocks to an output stream.
 */
abstract class AbstractTextFormatter extends AbstractFormatter {
    /**
     * The number of digits of precision to use when formatting the processing time
     */
    private static final int MILLIS_FORMAT_SCALE = 3;

    /**
     * Text used as separator when printing block text
     */
    private static final String BLOCK_TEXT_MARKER = "=====================================================================";

    /**
     * Constructor.
     *
     * @param out         The output stream to which messages are logged
     * @param printBanner Should the copyright banner be printed
     */
    AbstractTextFormatter(final OutputStream out, final boolean printBanner) {
        super(out);

        if (printBanner) {
            println(Version.BANNER);
        }
    }

    @Override
    public final void startCheck(final Options options) {
        Objects.requireNonNull(options, "options");
        println(options.toString());
    }

    @Override
    public void startSet(final int lineCount, final String fingerprint) {
        println("Found " + lineCount + " duplicate lines with fingerprint " + fingerprint + " in the following files:");
    }

    @Override
    public void endSet(final String text) {
        if (text != null) {
            print(text);
            println(BLOCK_TEXT_MARKER);
        }
    }

    @Override
    public void endCheck(final CheckSummary summary) {
        println("Found " + summary.getDuplicateLineCount() + " duplicate lines in " + summary.getDuplicateBlockCount() + " blocks in " + summary.getDuplicateFileCount() + " files");

        println("Processed a total of " + summary.getTotalSignificantLineCount() + " significant (" + summary.getTotalRawLineCount() + " raw) lines in " + summary.getTotalFileCount() + " files");

        println("Processing time: " + BigDecimal.valueOf(summary.getProcessingTime(), MILLIS_FORMAT_SCALE) + "sec");

        super.endCheck(summary);
    }
}
