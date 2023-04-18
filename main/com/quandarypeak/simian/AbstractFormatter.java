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

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;

/**
 * Convenience base class that simply logs sets of duplicate blocks to an output stream.
 */
abstract class AbstractFormatter implements AuditListener {
    private final PrintStream out;

    /**
     * Constructor.
     *
     * @param out The output stream to which messages are logged
     */
    AbstractFormatter(final OutputStream out) {
        this.out = out instanceof PrintStream ? (PrintStream) out : new PrintStream(out);
    }

    @Override
    public void endCheck(final CheckSummary summary) {
        out.close();
    }

    @Override
    public void error(final File file, final Throwable e) {
        System.err.println("Error processing file " + file.getPath() + ": " + e.getMessage());
    }

    protected final void println(final String message) {
        Objects.requireNonNull(message, "message");
        out.println(message);
    }

    protected final void print(final String message) {
        Objects.requireNonNull(message, "message");
        out.print(message);
    }
}
