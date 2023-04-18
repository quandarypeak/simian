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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Prevents an output stream from being closed.
 */
final class IgnoreCloseOutputStream extends FilterOutputStream {
    IgnoreCloseOutputStream(final OutputStream outputStream) {
        super(outputStream);
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
