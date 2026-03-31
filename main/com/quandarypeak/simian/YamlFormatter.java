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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Simply logs sets of duplicate blocks to an output stream.
 */
final class YamlFormatter extends AbstractFormatter {
    private static final Pattern AMPERSAND = Pattern.compile("\"");
    private static final Pattern SLASH_NEWLINE = Pattern.compile("\\n");
    private static final Pattern NEWLINE = Pattern.compile("\n");

    /**
     * Constructor.
     *
     * @param out         The output stream to which messages are logged
     * @param printBanner Should the copyright banner be printed
     */
    YamlFormatter(final OutputStream out, final boolean printBanner) {
        super(out);

        println("---");

        if (printBanner) {
            print("# ");
            println(SLASH_NEWLINE.matcher(Version.BANNER).replaceAll("\n# "));
        }

        println("simian:");
        println("  version: \"" + Version.VERSION + '"');
        println("  checks:");
    }

    @Override
    public void startCheck(final Options options) {
        String prefix = "- ";

        for (final Entry<Option, Object> entry : options.getOptions().entrySet()) {
            println("    " + prefix + entry.getKey() + ": " + entry.getValue());
            prefix = "  ";
        }

        println("      sets:");
    }

    @Override
    public void fileProcessed(final SourceFile sourceFile) {
    }

    @Override
    public void startSet(final int lineCount, final String fingerprint) {
        println("        - lineCount: " + lineCount);
        println("        - fingerprint: \"" + fingerprint + '"');
        println("          blocks:");
    }

    @Override
    public void block(final Block block) {
        Objects.requireNonNull(block, "block");

        println("            - sourceFile: \"" + block.getSourceFile().getFilename() + '"');
        println("              startLineNumber: " + block.getStartLineNumber());
        println("              endLineNumber: " + block.getEndLineNumber());
    }

    @Override
    public void endSet(final String text) {
        if (text != null) {
            print("          text: \"");
            print(NEWLINE.matcher(AMPERSAND.matcher(text).replaceAll("\\\"")).replaceAll("\\\\n"));
            println("\"");
        }
    }

    @Override
    public void endCheck(final CheckSummary summary) {
        println("      summary:");
        println("        duplicateFileCount: " + summary.getDuplicateFileCount());
        println("        duplicateLineCount: " + summary.getDuplicateLineCount());
        println("        duplicateBlockCount: " + summary.getDuplicateBlockCount());
        println("        totalFileCount: " + summary.getTotalFileCount());
        println("        totalRawLineCount: " + summary.getTotalRawLineCount());
        println("        totalSignificantLineCount: " + summary.getTotalSignificantLineCount());
        println("        processingTime: " + summary.getProcessingTime());

        super.endCheck(summary);
    }
}
