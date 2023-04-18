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

import java.io.OutputStream;

/**
 * Simply logs sets of duplicate blocks to an output stream.
 */
final class PlainFormatter extends AbstractTextFormatter {
    /**
     * Constructor.
     *
     * @param out         The output stream to which messages are logged
     * @param printBanner Should the copyright banner be printed
     */
    PlainFormatter(final OutputStream out, final boolean printBanner) {
        super(out, printBanner);
    }

    @Override
    public void fileProcessed(final SourceFile sourceFile) {
    }

    @Override
    public void block(final Block block) {
        println(" Between lines " + block.getStartLineNumber() + " and " + block.getEndLineNumber() + " in " + block.getSourceFile().getFilename());
    }
}
