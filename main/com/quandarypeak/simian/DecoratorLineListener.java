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

import java.util.Objects;

/**
 * Base class for line listeners that wish to add some specific behaviour such as line filtering, keyword filtering,
 * etc. By default simply delegates all calls to the decorated line listener.
 */
class DecoratorLineListener implements LineListener {
    private final LineListener _decorated;

    DecoratorLineListener(final LineListener decorated) {
        _decorated = Objects.requireNonNull(decorated, "decorated");
    }

    @Override
    public void file() {
        _decorated.file();
    }

    @Override
    public void line(final int lineNumber, final LineBuffer line) {
        _decorated.line(lineNumber, line);
    }
}
